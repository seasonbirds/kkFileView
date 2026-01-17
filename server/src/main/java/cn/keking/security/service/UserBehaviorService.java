package cn.keking.security.service;

import cn.keking.security.config.SecurityConfig;
import cn.keking.security.dao.UserBehaviorDao;
import cn.keking.security.model.UserBehaviorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户行为服务类
 * 负责检查用户请求频率、记录用户行为日志、实施访问限制
 * 基于Redis的高性能计数和基于数据库的持久化存储
 */
@Service
public class UserBehaviorService {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorService.class);

    /**
     * 用户行为数据访问对象
     */
    private final UserBehaviorDao userBehaviorDao;

    /**
     * 安全配置对象，包含频率限制阈值等配置
     */
    private final SecurityConfig securityConfig;

    /**
     * Redis访问计数服务，用于高性能的请求计数
     */
    private final RedisAccessCountService redisAccessCountService;

    public UserBehaviorService(UserBehaviorDao userBehaviorDao,
                               SecurityConfig securityConfig,
                               RedisAccessCountService redisAccessCountService) {
        this.userBehaviorDao = userBehaviorDao;
        this.securityConfig = securityConfig;
        this.redisAccessCountService = redisAccessCountService;
    }

    public static class CheckResult {
        private final boolean allowed;
        private final String message;
        private final boolean isDailyLimitExceeded;
        private final boolean isPeriodLimitExceeded;

        /**
         * 构造函数
         * @param allowed 是否允许访问
         * @param message 返回消息
         * @param isDailyLimitExceeded 是否超过每日限制
         * @param isPeriodLimitExceeded 是否超过周期限制
         */
        public CheckResult(boolean allowed, String message, boolean isDailyLimitExceeded, boolean isPeriodLimitExceeded) {
            this.allowed = allowed;
            this.message = message;
            this.isDailyLimitExceeded = isDailyLimitExceeded;
            this.isPeriodLimitExceeded = isPeriodLimitExceeded;
        }

        /**
         * 创建允许访问的结果
         */
        public static CheckResult allowed() {
            return new CheckResult(true, null, false, false);
        }

        /**
         * 创建超过每日限制的结果
         * @param message 错误消息
         */
        public static CheckResult dailyLimitExceeded(String message) {
            return new CheckResult(false, message, true, false);
        }

        /**
         * 创建超过周期限制的结果
         * @param message 错误消息
         */
        public static CheckResult periodLimitExceeded(String message) {
            return new CheckResult(false, message, false, true);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getMessage() {
            return message;
        }

        public boolean isDailyLimitExceeded() {
            return isDailyLimitExceeded;
        }

        public boolean isPeriodLimitExceeded() {
            return isPeriodLimitExceeded;
        }
    }

    /**
     * 检查并记录用户请求
     * 使用Redis进行高性能计数
     *
     * @param ipAddress 用户IP地址
     * @param fileName 请求的文件名
     * @return 检查结果
     */
    public CheckResult checkAndRecordRequest(String ipAddress, String fileName) {
        if (!securityConfig.isEnabled()) {
            return CheckResult.allowed();
        }

        int dailyCount = redisAccessCountService.getDailyCount(ipAddress);
        int periodCount = redisAccessCountService.getPeriodCount(ipAddress);

        if (dailyCount >= securityConfig.getDailyMaxRequests()) {
            String message = "用户行为异常，不能继续访问系统，请联系管理员！";
            logger.warn("IP {} exceeded daily limit: {} requests, limit: {}", ipAddress, dailyCount, securityConfig.getDailyMaxRequests());
            return CheckResult.dailyLimitExceeded(message);
        }

        if (periodCount >= securityConfig.getPeriodMaxRequests()) {
            String message = "请求太频繁，请稍后再试！";
            logger.warn("IP {} exceeded period limit: {} requests in {} minutes, limit: {}",
                    ipAddress, periodCount, securityConfig.getPeriodMinutes(), securityConfig.getPeriodMaxRequests());
            return CheckResult.periodLimitExceeded(message);
        }

        redisAccessCountService.incrementAndGetDailyCount(ipAddress);
        redisAccessCountService.incrementAndGetPeriodCount(ipAddress);

        UserBehaviorLog log = new UserBehaviorLog();
        log.setIpAddress(ipAddress);
        log.setFileName(fileName);
        log.setRequestTime(LocalDateTime.now());
        asyncInsertBehaviorLog(log);

        logger.debug("Recorded request from IP {} for file {}", ipAddress, fileName);
        return CheckResult.allowed();
    }

    /**
     * 清理指定天数之前的历史数据
     * @param daysToKeep 保留的天数
     */
    public void cleanupOldData(int daysToKeep) {
        userBehaviorDao.cleanupOldData(daysToKeep);
    }

    /**
     * 获取用户当天的访问次数
     * @param ipAddress 用户IP地址
     * @return 当天访问次数
     */
    public int getDailyAccessCount(String ipAddress) {
        return redisAccessCountService.getDailyCount(ipAddress);
    }

    /**
     * 获取用户在当前统计周期内的访问次数
     * @param ipAddress 用户IP地址
     * @return 统计周期内的访问次数
     */
    public int getPeriodAccessCount(String ipAddress) {
        return redisAccessCountService.getPeriodCount(ipAddress);
    }

    /**
     * 异步插入用户行为日志
     * 使用@Async注解实现异步执行，避免阻塞请求处理
     * @param log 用户行为日志对象
     */
    @Async
    public void asyncInsertBehaviorLog(UserBehaviorLog log) {
        try {
            userBehaviorDao.insertBehaviorLog(log);
        } catch (Exception e) {
            logger.error("Failed to insert behavior log asynchronously", e);
        }
    }
}
