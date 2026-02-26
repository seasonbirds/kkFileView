package cn.keking.service.impl;

import cn.keking.dao.UserBehaviorDao;
import cn.keking.model.UserAccessStats;
import cn.keking.model.UserBehaviorLog;
import cn.keking.service.UserBehaviorService;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户行为监控服务实现类
 * 使用本地缓存实现高性能访问统计
 * @author kkfileview
 */
@Service
public class UserBehaviorServiceImpl implements UserBehaviorService {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${behavior.monitor.cache.size:10000}")
    private Integer cacheSize;

    private final UserBehaviorDao userBehaviorDao;

    /**
     * 时间窗口请求计数器
     * key: ip_address
     * value: 请求计数器（包含计数和最后更新时间）
     */
    private ConcurrentMap<String, WindowCounter> windowCounters;

    /**
     * 每日统计缓存
     * key: ip_address#yyyy-MM-dd
     * value: 当日请求计数
     */
    private ConcurrentMap<String, AtomicInteger> dailyCounters;

    /**
     * 被阻止的IP缓存
     * key: ip_address#yyyy-MM-dd
     * value: 阻止时间戳
     */
    private ConcurrentMap<String, Long> blockedIps;

    private ScheduledExecutorService scheduler;

    public UserBehaviorServiceImpl(UserBehaviorDao userBehaviorDao) {
        this.userBehaviorDao = userBehaviorDao;
    }

    @PostConstruct
    public void init() {
        int maxSize = cacheSize != null ? cacheSize : 10000;

        this.windowCounters = new ConcurrentLinkedHashMap.Builder<String, WindowCounter>()
                .maximumWeightedCapacity(maxSize)
                .build();

        this.dailyCounters = new ConcurrentLinkedHashMap.Builder<String, AtomicInteger>()
                .maximumWeightedCapacity(maxSize)
                .build();

        this.blockedIps = new ConcurrentLinkedHashMap.Builder<String, Long>()
                .maximumWeightedCapacity(maxSize)
                .build();

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "user-behavior-cleanup");
            t.setDaemon(true);
            return t;
        });

        // 每分钟清理过期窗口数据
        this.scheduler.scheduleAtFixedRate(this::cleanupExpiredWindows, 1, 1, TimeUnit.MINUTES);

        logger.info("用户行为监控服务初始化完成，缓存大小: {}", maxSize);
    }

    @Override
    @Async("userBehaviorExecutor")
    public void logRequest(String ipAddress, String fileName, String requestUrl) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String today = now.format(DATE_FORMATTER);
            String dailyKey = ipAddress + "#" + today;

            // 更新时间窗口计数器
            WindowCounter counter = windowCounters.computeIfAbsent(ipAddress, k -> new WindowCounter());
            counter.increment();

            // 更新每日计数器
            dailyCounters.computeIfAbsent(dailyKey, k -> new AtomicInteger(0)).incrementAndGet();

            // 异步写入数据库
            UserBehaviorLog log = new UserBehaviorLog(ipAddress, fileName, now, requestUrl);
            userBehaviorDao.insertLog(log);
            userBehaviorDao.incrementDailyCount(ipAddress, today, 1);
        } catch (Exception e) {
            logger.error("记录用户请求失败", e);
        }
    }

    @Override
    public boolean isRateLimited(String ipAddress, int windowMinutes, int maxRequests) {
        WindowCounter counter = windowCounters.get(ipAddress);
        if (counter == null) {
            return false;
        }
        // 获取时间窗口内的请求次数
        int count = counter.getCount(windowMinutes);
        return count > maxRequests;
    }

    @Override
    public boolean isDailyLimited(String ipAddress, int dailyMaxRequests) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String dailyKey = ipAddress + "#" + today;

        // 检查是否已被阻止
        Long blockedTime = blockedIps.get(dailyKey);
        if (blockedTime != null) {
            return true;
        }

        AtomicInteger counter = dailyCounters.get(dailyKey);
        if (counter == null) {
            return false;
        }

        boolean limited = counter.get() > dailyMaxRequests;
        if (limited) {
            blockedIps.put(dailyKey, System.currentTimeMillis());
            // 异步更新数据库
            userBehaviorDao.setBlocked(ipAddress, today, true);
        }
        return limited;
    }

    @Override
    public int getRequestCountInWindow(String ipAddress, int windowMinutes) {
        WindowCounter counter = windowCounters.get(ipAddress);
        if (counter == null) {
            return 0;
        }
        return counter.getCount(windowMinutes);
    }

    @Override
    public List<UserBehaviorLog> getRecentLogs(String ipAddress, int windowMinutes) {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(windowMinutes);
        return userBehaviorDao.getRecentLogs(ipAddress, startTime);
    }

    @Override
    @Async("userBehaviorExecutor")
    public void incrementAlertCount(String ipAddress) {
        try {
            String today = LocalDate.now().format(DATE_FORMATTER);
            userBehaviorDao.incrementAlertCount(ipAddress, today);
        } catch (Exception e) {
            logger.error("增加告警次数失败", e);
        }
    }

    @Override
    public void cleanupOldData(LocalDateTime beforeTime) {
        userBehaviorDao.cleanupOldData(beforeTime);
    }

    @Override
    public UserAccessStats getTodayStats(String ipAddress) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        return userBehaviorDao.getOrCreateTodayStats(ipAddress, today);
    }

    private void cleanupExpiredWindows() {
        try {
            long now = System.currentTimeMillis();
            windowCounters.forEach((ip, counter) -> {
                counter.cleanupExpired(now);
            });
        } catch (Exception e) {
            logger.error("清理过期窗口数据失败", e);
        }
    }

    /**
     * 时间窗口计数器
     * 记录请求次数和每次请求的时间戳
     */
    private static class WindowCounter {
        // 请求次数
        private final AtomicInteger count = new AtomicInteger(0);
        // 窗口开始时间（毫秒）
        private volatile long windowStartTime = System.currentTimeMillis();
        // 最后请求时间（毫秒）
        private volatile long lastRequestTime = System.currentTimeMillis();

        /**
         * 增加请求计数
         */
        public void increment() {
            count.incrementAndGet();
            lastRequestTime = System.currentTimeMillis();
        }

        /**
         * 获取指定时间窗口内的请求次数
         * @param windowMinutes 时间窗口（分钟）
         * @return 请求次数
         */
        public int getCount(int windowMinutes) {
            long now = System.currentTimeMillis();
            long windowMillis = windowMinutes * 60 * 1000L;
            
            // 如果距离最后请求时间已超过窗口时间，说明窗口内无请求
            if (now - lastRequestTime > windowMillis) {
                return 0;
            }
            
            return count.get();
        }

        /**
         * 清理过期数据
         * @param now 当前时间戳
         */
        public void cleanupExpired(long now) {
            // 如果超过10分钟没有新请求，重置计数器
            if (now - lastRequestTime > 10 * 60 * 1000L) {
                count.set(0);
                windowStartTime = now;
            }
        }
    }
}
