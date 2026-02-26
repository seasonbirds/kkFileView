package cn.keking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户行为本地缓存
 * 用于限流计数器的本地缓存，避免每次限流检查都查询数据库
 *
 * 设计思路：
 * 1. 使用ConcurrentHashMap存储每个IP的访问信息
 * 2. CopyOnWriteArrayList记录访问时间，支持滑动窗口计数
 * 3. 后台线程定期清理过期IP，减少内存占用
 * 4. 支持从数据库初始化历史数据（可选）
 */
public class UserBehaviorCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserBehaviorCache.class);

    /** IP访问信息缓存 */
    private final Map<String, IpAccessInfo> accessCache = new ConcurrentHashMap<>();

    /** 清理过期缓存的定时调度器 */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "UserBehaviorCacheCleaner");
        t.setDaemon(true);
        return t;
    });

    /**
     * 构造函数
     * 启动定时清理任务，每60秒清理一次过期缓存
     */
    public UserBehaviorCache() {
        scheduler.scheduleAtFixedRate(this::cleanExpired, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 记录访问
     * @param ipAddress IP地址
     * @param periodMinutes 统计周期分钟数
     */
    public void recordAccess(String ipAddress, int periodMinutes) {
        accessCache.compute(ipAddress, (key, info) -> {
            if (info == null) {
                info = new IpAccessInfo();
            }
            info.recordAccess(periodMinutes);
            return info;
        });
    }

    /**
     * 获取周期内访问次数
     * @param ipAddress IP地址
     * @param periodMinutes 统计周期分钟数
     * @return 周期内访问次数
     */
    public int getPeriodCount(String ipAddress, int periodMinutes) {
        IpAccessInfo info = accessCache.get(ipAddress);
        if (info == null) {
            return 0;
        }
        return info.getPeriodCount(periodMinutes);
    }

    /**
     * 获取今日访问次数
     * @param ipAddress IP地址
     * @return 今日访问次数
     */
    public int getDailyCount(String ipAddress) {
        IpAccessInfo info = accessCache.get(ipAddress);
        if (info == null) {
            return 0;
        }
        return info.getDailyCount();
    }

    /**
     * 重置指定IP的今日计数
     * @param ipAddress IP地址
     */
    public void resetDailyCount(String ipAddress) {
        IpAccessInfo info = accessCache.get(ipAddress);
        if (info != null) {
            info.resetDailyCount();
        }
    }

    /**
     * 重置所有IP的今日计数
     * 每天0点调用
     */
    public void resetAllDailyCounts() {
        accessCache.values().forEach(IpAccessInfo::resetDailyCount);
        LOGGER.info("每日访问计数已重置");
    }

    /**
     * 清理过期IP信息
     * 超过60分钟未访问的IP将被清理
     */
    private void cleanExpired() {
        LocalDateTime now = LocalDateTime.now();
        int removed = 0;
        var iterator = accessCache.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpired(now)) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            LOGGER.debug("清理了 {} 个过期的IP缓存", removed);
        }
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * 从数据库初始化缓存数据
     * 用于服务重启后恢复限流状态
     * @param ipAddress IP地址
     * @param dailyCount 今日访问次数
     * @param periodCount 周期内访问次数
     * @param periodMinutes 统计周期分钟数
     */
    public void initializeFromDatabase(String ipAddress, int dailyCount, int periodCount, int periodMinutes) {
        accessCache.compute(ipAddress, (key, info) -> {
            if (info == null) {
                info = new IpAccessInfo();
            }
            info.initializeCounts(dailyCount, periodCount, periodMinutes);
            return info;
        });
    }

    /**
     * 单个IP的访问信息
     */
    private static class IpAccessInfo {
        /** IP信息过期时间（分钟） */
        private static final long CACHE_EXPIRE_MINUTES = 60;

        /** 访问时间列表，用于滑动窗口计数 */
        private final CopyOnWriteArrayList<LocalDateTime> accessTimes = new CopyOnWriteArrayList<>();
        /** 今日访问计数 */
        private final AtomicInteger dailyCount = new AtomicInteger(0);
        /** 最后访问时间 */
        private volatile LocalDateTime lastAccessTime = LocalDateTime.now();
        /** 最后访问日期 */
        private volatile LocalDate lastAccessDate = LocalDate.now();

        /**
         * 记录一次访问
         * @param periodMinutes 统计周期
         */
        public void recordAccess(int periodMinutes) {
            LocalDateTime now = LocalDateTime.now();
            this.lastAccessTime = now;

            // 跨天时重置
            LocalDate today = LocalDate.now();
            if (!lastAccessDate.equals(today)) {
                lastAccessDate = today;
                dailyCount.set(0);
                accessTimes.clear();
            }

            // 记录访问
            accessTimes.add(now);
            dailyCount.incrementAndGet();

            // 清理过期的访问时间记录（滑动窗口）
            LocalDateTime periodStart = now.minusMinutes(periodMinutes);
            accessTimes.removeIf(time -> time.isBefore(periodStart));
        }

        /**
         * 获取指定周期内的访问次数
         * @param periodMinutes 统计周期分钟数
         * @return 周期内访问次数
         */
        public int getPeriodCount(int periodMinutes) {
            LocalDateTime periodStart = LocalDateTime.now().minusMinutes(periodMinutes);
            return (int) accessTimes.stream()
                    .filter(time -> !time.isBefore(periodStart))
                    .count();
        }

        /**
         * 获取今日访问次数
         * @return 今日访问次数，跨天返回0
         */
        public int getDailyCount() {
            if (!lastAccessDate.equals(LocalDate.now())) {
                return 0;
            }
            return dailyCount.get();
        }

        /**
         * 重置今日计数
         */
        public void resetDailyCount() {
            dailyCount.set(0);
            accessTimes.clear();
            lastAccessDate = LocalDate.now();
        }

        /**
         * 判断是否过期
         * @param now 当前时间
         * @return 是否过期
         */
        public boolean isExpired(LocalDateTime now) {
            return lastAccessTime.plusMinutes(CACHE_EXPIRE_MINUTES).isBefore(now);
        }

        /**
         * 初始化计数值（从数据库恢复）
         * @param newDailyCount 今日计数
         * @param newPeriodCount 周期计数
         * @param periodMinutes 统计周期
         */
        public void initializeCounts(int newDailyCount, int newPeriodCount, int periodMinutes) {
            this.dailyCount.set(newDailyCount);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime periodStart = now.minusMinutes(periodMinutes);
            this.accessTimes.clear();
            // 模拟历史访问时间分布，每隔5秒一个
            for (int i = 0; i < newPeriodCount; i++) {
                this.accessTimes.add(periodStart.plusSeconds(i * 5));
            }
            this.lastAccessTime = now;
            this.lastAccessDate = LocalDate.now();
        }
    }
}
