package cn.keking.service.monitor;

import cn.keking.config.ConfigConstants;
import cn.keking.model.UserAccessLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 用户行为监控服务
 * 提供访问记录、限流判断、告警发送等功能
 */
@Service
public class UserBehaviorMonitorService {
    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorMonitorService.class);

    private final UserAccessRepository userAccessRepository;
    private final UserAccessCache userAccessCache;
    private final EmailService emailService;

    /**
     * 告警冷却时间记录：key=IP地址，value=上次告警时间戳
     */
    private final ConcurrentHashMap<String, Long> lastAlertTime = new ConcurrentHashMap<>();

    /**
     * 告警冷却时间（30分钟）
     */
    private static final long ALERT_COOLDOWN = TimeUnit.MINUTES.toMillis(30);

    public UserBehaviorMonitorService(UserAccessRepository userAccessRepository,
                                      UserAccessCache userAccessCache,
                                      EmailService emailService) {
        this.userAccessRepository = userAccessRepository;
        this.userAccessCache = userAccessCache;
        this.emailService = emailService;
    }

    /**
     * 初始化方法
     */
    @PostConstruct
    public void init() {
        if (ConfigConstants.isBehaviorMonitorEnabled()) {
            logger.info("User behavior monitor service initialized");
        }
    }

    /**
     * 记录用户访问（异步写入数据库，同步更新缓存）
     *
     * @param ipAddress IP地址
     * @param fileName  文件名称
     */
    public void recordAccess(String ipAddress, String fileName) {
        if (!ConfigConstants.isBehaviorMonitorEnabled()) {
            return;
        }

        // 同步更新缓存
        userAccessCache.recordAccess(ipAddress);

        // 异步写入数据库
        asyncSaveToDatabase(ipAddress, fileName);
    }

    /**
     * 异步保存访问记录到数据库
     *
     * @param ipAddress IP地址
     * @param fileName  文件名称
     */
    @Async("monitorTaskExecutor")
    public void asyncSaveToDatabase(String ipAddress, String fileName) {
        try {
            UserAccessLog log = new UserAccessLog(ipAddress, fileName, new Date());
            userAccessRepository.insert(log);
        } catch (Exception e) {
            logger.error("Failed to save access log to database", e);
        }
    }

    /**
     * 判断IP是否在时间窗口内超过请求阈值（使用缓存）
     *
     * @param ipAddress IP地址
     * @return true=超过阈值，false=未超过
     */
    public boolean isRateLimited(String ipAddress) {
        return userAccessCache.isRateLimited(ipAddress);
    }

    /**
     * 判断IP是否超过每日请求阈值（使用缓存）
     *
     * @param ipAddress IP地址
     * @return true=超过阈值，false=未超过
     */
    public boolean isDailyLimitExceeded(String ipAddress) {
        return userAccessCache.isDailyLimitExceeded(ipAddress);
    }

    /**
     * 发送告警邮件（如果需要）
     *
     * @param ipAddress IP地址
     */
    public void sendAlertIfNeeded(String ipAddress) {
        if (!ConfigConstants.isBehaviorMonitorEnabled()) {
            return;
        }

        // 检查告警冷却时间
        long now = System.currentTimeMillis();
        Long lastAlert = lastAlertTime.get(ipAddress);
        if (lastAlert != null && now - lastAlert < ALERT_COOLDOWN) {
            return;
        }

        int requestCount = userAccessCache.getWindowRequestCount(ipAddress);
        if (requestCount >= ConfigConstants.getBehaviorMonitorMaxRequests()) {
            int timeWindow = ConfigConstants.getBehaviorMonitorTimeWindow();
            // 异步发送邮件
            emailService.sendAlertEmail(ipAddress, requestCount, timeWindow);
            lastAlertTime.put(ipAddress, now);
        }
    }

}
