package cn.keking.audit.service;

import cn.keking.audit.config.AuditConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 本地缓存服务
 * 用于高性能的访问计数和IP封禁管理
 * 使用本地内存缓存，避免频繁访问数据库
 */
@Service
public class LocalCacheService {

    private static final Logger logger = LoggerFactory.getLogger(LocalCacheService.class);

    private final Map<String, WindowCounter> windowCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> dailyCounters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> blockedUntil = new ConcurrentHashMap<>();
    private final Map<String, LocalDate> dailyBlockedUntil = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastAlertTime = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @PostConstruct
    public void init() {
        logger.info("LocalCacheService initialized");
    }

    /**
     * 增加并获取统计周期内的访问计数
     *
     * @param ip 用户IP地址
     * @return 当前计数
     */
    public int incrementAndGetWindowCount(String ip) {
        WindowCounter counter = windowCounters.computeIfAbsent(ip, k -> new WindowCounter());
        return counter.incrementAndGet();
    }

    /**
     * 获取统计周期内的访问计数
     *
     * @param ip 用户IP地址
     * @return 当前计数
     */
    public int getWindowCount(String ip) {
        WindowCounter counter = windowCounters.get(ip);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 增加并获取每日访问计数
     *
     * @param ip 用户IP地址
     * @return 当前计数
     */
    public int incrementAndGetDailyCount(String ip) {
        AtomicInteger counter = dailyCounters.computeIfAbsent(ip, k -> new AtomicInteger(0));
        return counter.incrementAndGet();
    }

    /**
     * 获取每日访问计数
     *
     * @param ip 用户IP地址
     * @return 当前计数
     */
    public int getDailyCount(String ip) {
        AtomicInteger counter = dailyCounters.get(ip);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 封禁IP指定时间
     *
     * @param ip 用户IP地址
     * @param minutes 封禁时间（分钟）
     */
    public void blockIp(String ip, int minutes) {
        lock.writeLock().lock();
        try {
            blockedUntil.put(ip, LocalDateTime.now().plusMinutes(minutes));
            logger.info("IP {} blocked for {} minutes", ip, minutes);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 封禁IP到当天结束
     *
     * @param ip 用户IP地址
     */
    public void blockIpDaily(String ip) {
        lock.writeLock().lock();
        try {
            dailyBlockedUntil.put(ip, LocalDate.now());
            logger.info("IP {} blocked for the rest of the day", ip);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 检查IP是否被封禁
     * 包括周期封禁和每日封禁
     *
     * @param ip 用户IP地址
     * @return true表示被封禁
     */
    public boolean isBlocked(String ip) {
        lock.readLock().lock();
        try {
            if (isDailyBlocked(ip)) {
                return true;
            }
            LocalDateTime blocked = blockedUntil.get(ip);
            return blocked != null && blocked.isAfter(LocalDateTime.now());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检查IP是否被每日封禁
     *
     * @param ip 用户IP地址
     * @return true表示被封禁
     */
    public boolean isDailyBlocked(String ip) {
        lock.readLock().lock();
        try {
            LocalDate blockedDate = dailyBlockedUntil.get(ip);
            return blockedDate != null && blockedDate.equals(LocalDate.now());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检查是否应该发送告警邮件
     * 考虑告警冷却时间
     *
     * @param ip 用户IP地址
     * @return true表示应该发送告警
     */
    public boolean shouldSendAlert(String ip) {
        lock.readLock().lock();
        try {
            LocalDateTime lastAlert = lastAlertTime.get(ip);
            if (lastAlert == null) {
                return true;
            }
            int cooldownMinutes = AuditConfig.getAlertCooldownMinutes();
            return lastAlert.plusMinutes(cooldownMinutes).isBefore(LocalDateTime.now());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 更新最后告警时间
     *
     * @param ip 用户IP地址
     */
    public void updateLastAlertTime(String ip) {
        lock.writeLock().lock();
        try {
            lastAlertTime.put(ip, LocalDateTime.now());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 窗口计数器内部类
     * 用于统计周期内的访问计数
     */
    private static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile LocalDateTime lastUpdateTime = LocalDateTime.now();

        /**
         * 增加计数并更新最后更新时间
         *
         * @return 当前计数
         */
        public int incrementAndGet() {
            lastUpdateTime = LocalDateTime.now();
            return count.incrementAndGet();
        }

        /**
         * 获取当前计数
         *
         * @return 当前计数
         */
        public int get() {
            return count.get();
        }

        /**
         * 获取最后更新时间
         *
         * @return 最后更新时间
         */
        public LocalDateTime getLastUpdateTime() {
            return lastUpdateTime;
        }
    }
}
