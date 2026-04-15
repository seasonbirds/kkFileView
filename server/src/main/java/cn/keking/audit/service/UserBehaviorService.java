package cn.keking.audit.service;

import cn.keking.audit.config.AuditConfig;
import cn.keking.audit.model.UserBehaviorLog;
import cn.keking.audit.repository.UserBehaviorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户行为服务
 * 用于记录用户行为和进行访问控制检查
 */
@Service
public class UserBehaviorService {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorService.class);

    private final UserBehaviorLogRepository userBehaviorLogRepository;
    private final LocalCacheService localCacheService;
    private final EmailAlertService emailAlertService;

    public UserBehaviorService(UserBehaviorLogRepository userBehaviorLogRepository,
                                LocalCacheService localCacheService,
                                EmailAlertService emailAlertService) {
        this.userBehaviorLogRepository = userBehaviorLogRepository;
        this.localCacheService = localCacheService;
        this.emailAlertService = emailAlertService;
    }

    /**
     * 异步记录用户行为
     *
     * @param ip 用户IP地址
     * @param fileName 文件名
     * @param requestUri 请求URI
     * @param requestMethod 请求方法
     */
    @Async("auditTaskExecutor")
    @Transactional
    public void recordUserBehaviorAsync(String ip, String fileName, String requestUri, String requestMethod) {
        try {
            UserBehaviorLog log = new UserBehaviorLog(
                ip,
                fileName,
                LocalDateTime.now(),
                requestUri,
                requestMethod
            );
            userBehaviorLogRepository.save(log);

            logger.debug("Recorded user behavior: IP={}, File={}", ip, fileName);
        } catch (Exception e) {
            logger.error("Failed to record user behavior: IP={}", ip, e);
        }
    }

    /**
     * 检查访问是否允许
     * 进行周期访问限制和每日访问限制检查
     *
     * @param ip 用户IP地址
     * @return 访问检查结果
     */
    public AccessCheckResult checkAccess(String ip) {
        if (localCacheService.isDailyBlocked(ip)) {
            return new AccessCheckResult(false, "用户行为异常，不能继续访问系统，请联系管理员！");
        }

        if (localCacheService.isBlocked(ip)) {
            return new AccessCheckResult(false, "请求太频繁，请稍后再试！");
        }

        int windowCount = localCacheService.incrementAndGetWindowCount(ip);
        int dailyCount = localCacheService.incrementAndGetDailyCount(ip);

        int maxPerWindow = AuditConfig.getMaxRequestsPerWindow();
        int maxPerDay = AuditConfig.getMaxRequestsPerDay();
        int timeWindowMinutes = AuditConfig.getTimeWindowMinutes();

        if (dailyCount > maxPerDay) {
            localCacheService.blockIpDaily(ip);
            logger.warn("IP {} exceeded daily limit: {} > {}", ip, dailyCount, maxPerDay);

            if (localCacheService.shouldSendAlert(ip)) {
                emailAlertService.sendAlertAsync(ip, dailyCount, timeWindowMinutes, "DAILY_LIMIT");
                localCacheService.updateLastAlertTime(ip);
            }

            return new AccessCheckResult(false, "用户行为异常，不能继续访问系统，请联系管理员！");
        }

        if (windowCount > maxPerWindow) {
            localCacheService.blockIp(ip, timeWindowMinutes);
            logger.warn("IP {} exceeded window limit: {} > {} in {} minutes",
                ip, windowCount, maxPerWindow, timeWindowMinutes);

            if (localCacheService.shouldSendAlert(ip)) {
                emailAlertService.sendAlertAsync(ip, windowCount, timeWindowMinutes, "WINDOW_LIMIT");
                localCacheService.updateLastAlertTime(ip);
            }

            return new AccessCheckResult(false, "请求太频繁，请稍后再试！");
        }

        return new AccessCheckResult(true, null);
    }

    /**
     * 访问检查结果类
     */
    public static class AccessCheckResult {
        private final boolean allowed;
        private final String rejectMessage;

        public AccessCheckResult(boolean allowed, String rejectMessage) {
            this.allowed = allowed;
            this.rejectMessage = rejectMessage;
        }

        /**
         * 检查是否允许访问
         *
         * @return true表示允许访问
         */
        public boolean isAllowed() {
            return allowed;
        }

        /**
         * 获取拒绝消息
         *
         * @return 拒绝消息
         */
        public String getRejectMessage() {
            return rejectMessage;
        }
    }
}
