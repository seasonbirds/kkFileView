package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.model.UserBehaviorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用户行为分析服务
 *
 * @author keking
 * @since 2025-07-17
 */
@Service
public class UserBehaviorService {
    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorService.class);
    private final RedisUserBehaviorHelper redisHelper;
    private final EmailAlertService emailAlertService;
    private final UserBehaviorLogService userBehaviorLogService;
    private final ExecutorService emailExecutor;

    public UserBehaviorService() {
        this.redisHelper = RedisUserBehaviorHelper.getInstance();
        this.emailAlertService = new EmailAlertService();
        this.userBehaviorLogService = new UserBehaviorLogService();
        this.emailExecutor = Executors.newFixedThreadPool(2);
    }

    /**
     * 处理用户预览请求
     *
     * @param ipAddress 请求IP地址
     * @param fileName  文件名
     * @param requestUrl 请求URL
     * @return 处理结果
     */
    public UserBehaviorResult handleRequest(String ipAddress, String fileName, String requestUrl) {
        // 检查功能是否启用
        if (!ConfigConstants.isUserBehaviorAnalysisEnabled()) {
            return UserBehaviorResult.ALLOWED;
        }

        // 检查当天访问阈值
        int dailyThreshold = ConfigConstants.getUserBehaviorAnalysisDailyThreshold();
        if (dailyThreshold > 0) {
            int todayCount = redisHelper.getDailyRequestCount(ipAddress);
            if (todayCount >= dailyThreshold) {
                logger.warn("IP地址 {} 当天访问次数超过阈值: {}/{}, 已禁止访问",
                        ipAddress, todayCount, dailyThreshold);
                return UserBehaviorResult.DAILY_LIMIT_EXCEEDED;
            }
        }

        // 检查周期访问阈值
        int periodMinutes = ConfigConstants.getUserBehaviorAnalysisPeriodMinutes();
        int periodThreshold = ConfigConstants.getUserBehaviorAnalysisThresholdPerPeriod();
        if (periodMinutes > 0 && periodThreshold > 0) {
            int periodCount = redisHelper.getPeriodRequestCount(ipAddress);
            if (periodCount >= periodThreshold) {
                logger.warn("IP地址 {} 在 {} 分钟内访问次数超过阈值: {}/{}",
                        ipAddress, periodMinutes, periodCount, periodThreshold);

                // 发送告警邮件（异步）
                String subject = "用户行为异常";
                String content = String.format("IP地址 %s 在过去 %d 分钟内访问了 %d 次系统，超出正常范围，请保持关注。",
                        ipAddress, periodMinutes, periodCount);
                emailExecutor.submit(() -> emailAlertService.sendAlert(subject, content));

                return UserBehaviorResult.PERIOD_LIMIT_EXCEEDED;
            }
        }

        // 记录用户行为日志
        UserBehaviorLog log = new UserBehaviorLog();
        log.setIpAddress(ipAddress);
        log.setFileName(fileName);
        log.setRequestTime(LocalDateTime.now());
        log.setRequestUrl(requestUrl);
        userBehaviorLogService.saveLog(log);

        // 更新Redis统计
        redisHelper.incrementRequestCount(ipAddress, periodMinutes);

        return UserBehaviorResult.ALLOWED;
    }

    /**
     * 用户行为处理结果枚举
     */
    public enum UserBehaviorResult {
        ALLOWED(""),
        PERIOD_LIMIT_EXCEEDED("请求太频繁，请稍后再试！"),
        DAILY_LIMIT_EXCEEDED("用户行为异常，不能继续访问系统，请联系管理员！");

        private final String message;

        UserBehaviorResult(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
