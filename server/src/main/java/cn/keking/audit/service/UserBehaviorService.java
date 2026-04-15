package cn.keking.audit.service;

import cn.keking.audit.config.AuditConfig;
import cn.keking.audit.model.DailyAccessCount;
import cn.keking.audit.model.UserBehaviorLog;
import cn.keking.audit.repository.DailyAccessCountRepository;
import cn.keking.audit.repository.UserBehaviorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserBehaviorService {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorService.class);

    private final UserBehaviorLogRepository userBehaviorLogRepository;
    private final DailyAccessCountRepository dailyAccessCountRepository;
    private final LocalCacheService localCacheService;
    private final EmailAlertService emailAlertService;

    public UserBehaviorService(UserBehaviorLogRepository userBehaviorLogRepository,
                                DailyAccessCountRepository dailyAccessCountRepository,
                                LocalCacheService localCacheService,
                                EmailAlertService emailAlertService) {
        this.userBehaviorLogRepository = userBehaviorLogRepository;
        this.dailyAccessCountRepository = dailyAccessCountRepository;
        this.localCacheService = localCacheService;
        this.emailAlertService = emailAlertService;
    }

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
            
            updateDailyCount(ip);
            
            logger.debug("Recorded user behavior: IP={}, File={}", ip, fileName);
        } catch (Exception e) {
            logger.error("Failed to record user behavior: IP={}", ip, e);
        }
    }

    @Transactional
    public void updateDailyCount(String ip) {
        LocalDate today = LocalDate.now();
        Optional<DailyAccessCount> existing = dailyAccessCountRepository.findByIpAndAccessDate(ip, today);
        
        if (existing.isPresent()) {
            DailyAccessCount count = existing.get();
            count.incrementCount();
            dailyAccessCountRepository.save(count);
        } else {
            DailyAccessCount newCount = new DailyAccessCount(ip, today, 1);
            dailyAccessCountRepository.save(newCount);
        }
    }

    public AccessCheckResult checkAccess(String ip) {
        if (!AuditConfig.isAuditEnabled()) {
            return new AccessCheckResult(true, null);
        }

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

    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void cleanOldLogs() {
        int retentionDays = AuditConfig.getLogRetentionDays();
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        
        try {
            userBehaviorLogRepository.deleteByRequestTimeBefore(cutoffTime);
            logger.info("Cleaned user behavior logs older than {} days", retentionDays);
        } catch (Exception e) {
            logger.error("Failed to clean old logs", e);
        }
    }

    public static class AccessCheckResult {
        private final boolean allowed;
        private final String rejectMessage;

        public AccessCheckResult(boolean allowed, String rejectMessage) {
            this.allowed = allowed;
            this.rejectMessage = rejectMessage;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getRejectMessage() {
            return rejectMessage;
        }
    }
}
