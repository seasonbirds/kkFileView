package cn.keking.audit.service;

import cn.keking.audit.config.AuditConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    public int incrementAndGetWindowCount(String ip) {
        WindowCounter counter = windowCounters.computeIfAbsent(ip, k -> new WindowCounter());
        return counter.incrementAndGet();
    }

    public int getWindowCount(String ip) {
        WindowCounter counter = windowCounters.get(ip);
        return counter != null ? counter.get() : 0;
    }

    public int incrementAndGetDailyCount(String ip) {
        AtomicInteger counter = dailyCounters.computeIfAbsent(ip, k -> new AtomicInteger(0));
        return counter.incrementAndGet();
    }

    public int getDailyCount(String ip) {
        AtomicInteger counter = dailyCounters.get(ip);
        return counter != null ? counter.get() : 0;
    }

    public void blockIp(String ip, int minutes) {
        lock.writeLock().lock();
        try {
            blockedUntil.put(ip, LocalDateTime.now().plusMinutes(minutes));
            logger.info("IP {} blocked for {} minutes", ip, minutes);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void blockIpDaily(String ip) {
        lock.writeLock().lock();
        try {
            dailyBlockedUntil.put(ip, LocalDate.now());
            logger.info("IP {} blocked for the rest of the day", ip);
        } finally {
            lock.writeLock().unlock();
        }
    }

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

    public boolean isDailyBlocked(String ip) {
        lock.readLock().lock();
        try {
            LocalDate blockedDate = dailyBlockedUntil.get(ip);
            return blockedDate != null && blockedDate.equals(LocalDate.now());
        } finally {
            lock.readLock().unlock();
        }
    }

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

    public void updateLastAlertTime(String ip) {
        lock.writeLock().lock();
        try {
            lastAlertTime.put(ip, LocalDateTime.now());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Scheduled(fixedRate = 60000)
    public void cleanExpiredCounters() {
        logger.debug("Cleaning expired counters");
        LocalDateTime now = LocalDateTime.now();
        int timeWindowMinutes = AuditConfig.getTimeWindowMinutes();
        
        windowCounters.entrySet().removeIf(entry -> {
            WindowCounter counter = entry.getValue();
            return counter.getLastUpdateTime().plusMinutes(timeWindowMinutes).isBefore(now);
        });

        lock.writeLock().lock();
        try {
            blockedUntil.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
            
            LocalDate today = LocalDate.now();
            dailyBlockedUntil.entrySet().removeIf(entry -> !entry.getValue().equals(today));
            
            lastAlertTime.entrySet().removeIf(entry -> 
                entry.getValue().plusMinutes(AuditConfig.getAlertCooldownMinutes() * 2).isBefore(now));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyCounters() {
        logger.info("Resetting daily counters");
        dailyCounters.clear();
        lock.writeLock().lock();
        try {
            dailyBlockedUntil.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile LocalDateTime lastUpdateTime = LocalDateTime.now();

        public int incrementAndGet() {
            lastUpdateTime = LocalDateTime.now();
            return count.incrementAndGet();
        }

        public int get() {
            return count.get();
        }

        public LocalDateTime getLastUpdateTime() {
            return lastUpdateTime;
        }
    }
}
