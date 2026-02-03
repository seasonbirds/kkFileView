package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.repository.UserBehaviorRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

/**
 * 用户行为监控服务
 * 负责业务逻辑处理：频率限制、日访问限制、告警触发等
 *
 * @author kkFileView
 */
@Service
public class UserBehaviorService {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserBehaviorRepository userBehaviorRepository;
    private final EmailService emailService;

    private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "user-behavior-async");
        t.setDaemon(true);
        return t;
    });

    // 缓存，用于减少数据库查询频率
    private final ConcurrentHashMap<String, AccessCache> accessCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DailyAccessCache> dailyAccessCache = new ConcurrentHashMap<>();

    // 缓存清理定时器
    private final ScheduledExecutorService cacheCleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cache-cleaner");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    public UserBehaviorService(UserBehaviorRepository userBehaviorRepository, EmailService emailService) {
        this.userBehaviorRepository = userBehaviorRepository;
        this.emailService = emailService;
    }

    @PostConstruct
    public void init() {
        if (!ConfigConstants.isUserBehaviorMonitorEnabled()) {
            logger.info("用户行为监控功能未启用");
            return;
        }
        startCacheCleaner();
        logger.info("用户行为监控服务初始化完成");
    }

    @PreDestroy
    public void destroy() {
        asyncExecutor.shutdown();
        cacheCleaner.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
            if (!cacheCleaner.awaitTermination(5, TimeUnit.SECONDS)) {
                cacheCleaner.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            cacheCleaner.shutdownNow();
        }
    }

    /**
     * 启动缓存清理定时器
     */
    private void startCacheCleaner() {
        cacheCleaner.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                String today = LocalDate.now().format(DATE_FORMATTER);

                // 清理过期的分钟级缓存
                accessCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));

                // 清理非当天的日访问缓存
                dailyAccessCache.entrySet().removeIf(entry -> !entry.getValue().getDate().equals(today));

            } catch (Exception e) {
                logger.error("缓存清理异常", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 记录访问请求
     */
    public void recordAccess(String ipAddress, String fileName, String requestUrl, String userAgent) {
        if (!ConfigConstants.isUserBehaviorMonitorEnabled()) {
            return;
        }

        if (!userBehaviorRepository.isConnectionAvailable()) {
            logger.warn("数据库连接不可用，跳过访问记录");
            return;
        }

        asyncExecutor.submit(() -> {
            try {
                String today = LocalDate.now().format(DATE_FORMATTER);

                // 插入访问记录
                userBehaviorRepository.insertAccessLog(ipAddress, fileName, requestUrl, userAgent);

                // 更新每日统计
                userBehaviorRepository.updateDailyStats(ipAddress, today);

            } catch (SQLException e) {
                logger.error("记录访问日志失败", e);
            }
        });
    }

    /**
     * 检查是否需要拦截请求
     * @return 拦截原因，如果不需要拦截则返回null
     */
    public String checkAccessLimit(String ipAddress) {
        if (!ConfigConstants.isUserBehaviorMonitorEnabled()) {
            return null;
        }

        if (!userBehaviorRepository.isConnectionAvailable()) {
            logger.warn("数据库连接不可用，跳过访问限制检查");
            return null;
        }

        try {
            // 1. 检查日访问阈值（优先级更高）
            String dailyLimitResult = checkDailyLimit(ipAddress);
            if (dailyLimitResult != null) {
                return dailyLimitResult;
            }

            // 2. 检查分钟级频率限制
            String minuteLimitResult = checkMinuteLimit(ipAddress);
            if (minuteLimitResult != null) {
                return minuteLimitResult;
            }

            return null;
        } catch (SQLException e) {
            logger.error("检查访问限制异常", e);
            return null; // 异常时不拦截，保证服务可用性
        }
    }

    /**
     * 检查日访问阈值
     */
    private String checkDailyLimit(String ipAddress) throws SQLException {
        int dailyThreshold = ConfigConstants.getUserBehaviorDailyThreshold();
        if (dailyThreshold <= 0) {
            return null;
        }

        String today = LocalDate.now().format(DATE_FORMATTER);

        // 先检查缓存
        DailyAccessCache cache = dailyAccessCache.get(ipAddress);
        if (cache != null && cache.getDate().equals(today)) {
            if (cache.getCount() >= dailyThreshold) {
                return "用户行为异常，不能继续访问系统，请联系管理员！";
            }
            cache.increment();
            return null;
        }

        // 查询数据库
        int count = userBehaviorRepository.getDailyAccessCount(ipAddress, today);

        // 更新缓存
        dailyAccessCache.put(ipAddress, new DailyAccessCache(today, count + 1));

        if (count >= dailyThreshold) {
            return "用户行为异常，不能继续访问系统，请联系管理员！";
        }

        return null;
    }

    /**
     * 检查分钟级频率限制
     */
    private String checkMinuteLimit(String ipAddress) throws SQLException {
        int timeWindow = ConfigConstants.getUserBehaviorTimeWindow(); // 分钟
        int threshold = ConfigConstants.getUserBehaviorThreshold(); // 阈值

        if (timeWindow <= 0 || threshold <= 0) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        String cacheKey = ipAddress + ":" + now.getHour() + ":" + now.getMinute() / timeWindow;

        // 检查缓存
        AccessCache cache = accessCache.get(cacheKey);
        if (cache != null) {
            if (cache.isBlocked()) {
                return "请求太频繁，请稍后再试！";
            }

            int count = cache.incrementAndGet();
            if (count > threshold) {
                cache.setBlocked(true);
                // 发送告警邮件
                sendAlertEmail(ipAddress, count, threshold, timeWindow);
                return "请求太频繁，请稍后再试！";
            }
            return null;
        }

        // 查询数据库统计当前时间窗口内的请求数
        LocalDateTime windowStart = now.minusMinutes(timeWindow);
        int count = userBehaviorRepository.getAccessCountInTimeWindow(ipAddress, windowStart);

        // 创建新缓存
        AccessCache newCache = new AccessCache(now.plusMinutes(timeWindow));
        newCache.incrementAndGet();
        accessCache.put(cacheKey, newCache);

        if (count >= threshold) {
            newCache.setBlocked(true);
            // 发送告警邮件
            sendAlertEmail(ipAddress, count, threshold, timeWindow);
            return "请求太频繁，请稍后再试！";
        }

        return null;
    }

    /**
     * 发送告警邮件
     */
    private void sendAlertEmail(String ipAddress, int accessCount, int threshold, int timeWindow) {
        asyncExecutor.submit(() -> {
            try {
                // 检查是否已经发送过告警（避免重复发送）
                LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
                if (userBehaviorRepository.isAlertSentRecently(ipAddress, "FREQUENCY", fiveMinutesAgo)) {
                    return;
                }

                String message = String.format("IP地址%s在过去%d分钟内访问了%d次系统，超出正常范围，请保持关注", 
                        ipAddress, timeWindow, accessCount);

                // 记录告警日志
                userBehaviorRepository.insertAlertLog(ipAddress, "FREQUENCY", accessCount, threshold, message);

                // 发送邮件
                emailService.sendAlertEmail("用户行为异常", message);

                logger.warn("用户行为异常告警已发送 - IP: {}, 访问次数: {}, 阈值: {}", ipAddress, accessCount, threshold);

            } catch (Exception e) {
                logger.error("发送告警邮件失败", e);
            }
        });
    }

    // ==================== 缓存内部类 ====================

    /**
     * 分钟级访问缓存
     */
    private static class AccessCache {
        private final LocalDateTime expireTime;
        private int count = 0;
        private volatile boolean blocked = false;

        public AccessCache(LocalDateTime expireTime) {
            this.expireTime = expireTime;
        }

        public synchronized int incrementAndGet() {
            return ++count;
        }

        public boolean isExpired(LocalDateTime now) {
            return now.isAfter(expireTime);
        }

        public boolean isBlocked() {
            return blocked;
        }

        public void setBlocked(boolean blocked) {
            this.blocked = blocked;
        }
    }

    /**
     * 日访问缓存
     */
    private static class DailyAccessCache {
        private final String date;
        private int count;

        public DailyAccessCache(String date, int count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() {
            return date;
        }

        public int getCount() {
            return count;
        }

        public void increment() {
            this.count++;
        }
    }
}
