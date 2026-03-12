package cn.keking.service.monitor;

import cn.keking.config.ConfigConstants;
import cn.keking.model.monitor.BlockedIp;
import cn.keking.repository.monitor.BlockedIpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 访问计数器，使用ConcurrentHashMap实现带过期的本地缓存
 * 采用内存计数和内存封禁状态检查，避免频繁查询数据库
 */
@Component
public class AccessCounter {

    private static final Logger logger = LoggerFactory.getLogger(AccessCounter.class);

    private final BlockedIpRepository blockedIpRepository;

    /**
     * 时间窗口内的访问计数缓存
     */
    private ConcurrentHashMap<String, CounterEntry> windowCounter;

    /**
     * 每日访问计数
     */
    private ConcurrentHashMap<String, AtomicLong> dailyCounter;

    /**
     * 封禁IP的内存缓存（避免每次查询数据库）
     * key: IP地址
     * value: 封禁信息
     */
    private ConcurrentHashMap<String, BlockInfo> blockedIpCache;

    /**
     * 当前计数的日期
     */
    private volatile LocalDate currentDate;

    /**
     * 封禁信息内部类
     */
    public static class BlockInfo {
        private final LocalDateTime blockEndTime;
        private final String reason;

        public BlockInfo(LocalDateTime blockEndTime, String reason) {
            this.blockEndTime = blockEndTime;
            this.reason = reason;
        }

        public LocalDateTime getBlockEndTime() {
            return blockEndTime;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * 计数条目包装类
     */
    private static class CounterEntry {
        final AtomicLong count;
        volatile long lastAccessTime;

        CounterEntry() {
            this.count = new AtomicLong(0);
            this.lastAccessTime = System.currentTimeMillis();
        }

        long incrementAndGet() {
            lastAccessTime = System.currentTimeMillis();
            return count.incrementAndGet();
        }

        long get() {
            return count.get();
        }
    }

    public AccessCounter(BlockedIpRepository blockedIpRepository) {
        this.blockedIpRepository = blockedIpRepository;
    }

    @PostConstruct
    public void init() {
        currentDate = LocalDate.now();
        windowCounter = new ConcurrentHashMap<>();
        dailyCounter = new ConcurrentHashMap<>();
        blockedIpCache = new ConcurrentHashMap<>();

        // 启动时从数据库加载已封禁的IP
        loadBlockedIpsFromDatabase();

        logger.info("AccessCounter initialized with ConcurrentHashMap");
    }

    /**
     * 从数据库加载所有活跃的封禁IP到内存缓存
     */
    public void loadBlockedIpsFromDatabase() {
        try {
            List<BlockedIp> activeBlocks = blockedIpRepository.findAllActiveBlocks(LocalDateTime.now());
            for (BlockedIp blockedIp : activeBlocks) {
                blockedIpCache.put(blockedIp.getIpAddress(),
                        new BlockInfo(blockedIp.getBlockEndTime(), blockedIp.getReason()));
            }
            logger.info("从数据库加载了 {} 个活跃封禁IP", activeBlocks.size());
        } catch (Exception e) {
            logger.error("加载封禁IP列表失败", e);
        }
    }

    /**
     * 定时清理过期的时间窗口计数和封禁缓存（每分钟执行一次）
     */
    @Scheduled(fixedRate = 60000)
    public void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        LocalDateTime nowDateTime = LocalDateTime.now();
        long expireMillis = ConfigConstants.getMonitorFrequencyWindowMinutes() * 60 * 1000L;
        int removedCount = 0;
        int unblockedCount = 0;

        // 清理过期的时间窗口计数
        for (java.util.Iterator<java.util.Map.Entry<String, CounterEntry>> it = windowCounter.entrySet().iterator(); it.hasNext(); ) {
            java.util.Map.Entry<String, CounterEntry> entry = it.next();
            if (now - entry.getValue().lastAccessTime > expireMillis) {
                it.remove();
                removedCount++;
            }
        }

        // 清理已解封的IP缓存
        for (java.util.Iterator<java.util.Map.Entry<String, BlockInfo>> it = blockedIpCache.entrySet().iterator(); it.hasNext(); ) {
            java.util.Map.Entry<String, BlockInfo> entry = it.next();
            if (nowDateTime.isAfter(entry.getValue().getBlockEndTime())) {
                it.remove();
                unblockedCount++;
            }
        }

        if (removedCount > 0 || unblockedCount > 0) {
            logger.debug("清理了 {} 个过期计数, {} 个解封IP", removedCount, unblockedCount);
        }
    }

    /**
     * 检查并重置每日计数器（跨天的时候）
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyCounter() {
        dailyCounter.clear();
        windowCounter.clear();
        currentDate = LocalDate.now();
        logger.info("每日计数器已重置，新日期: {}", currentDate);
    }

    private void checkAndResetDailyCounter() {
        LocalDate now = LocalDate.now();
        if (!now.equals(currentDate)) {
            resetDailyCounter();
        }
    }

    /**
     * 获取封禁信息（内存检查，性能优先）
     * @param ipAddress IP地址
     * @return 封禁信息，如果未封禁或已过期返回null
     */
    public BlockInfo getBlockInfo(String ipAddress) {
        BlockInfo blockInfo = blockedIpCache.get(ipAddress);
        if (blockInfo != null) {
            if (LocalDateTime.now().isBefore(blockInfo.getBlockEndTime())) {
                return blockInfo;
            } else {
                blockedIpCache.remove(ipAddress);
            }
        }
        return null;
    }

    /**
     * 将IP标记为封禁（内存缓存）
     * @param ipAddress IP地址
     * @param blockEndTime 封禁结束时间
     * @param reason 封禁原因
     */
    public void markIpBlocked(String ipAddress, LocalDateTime blockEndTime, String reason) {
        blockedIpCache.put(ipAddress, new BlockInfo(blockEndTime, reason));
    }

    /**
     * 增加计数并检查是否超过限制
     * 核心方法：一次调用完成计数和检查
     * 注意：调用此方法前必须先调用getBlockInfo检查是否已封禁
     * @param ipAddress IP地址
     * @return 检查结果
     */
    public LimitCheckResult incrementAndCheck(String ipAddress) {
        checkAndResetDailyCounter();

        // 增加时间窗口计数
        CounterEntry windowEntry = windowCounter.computeIfAbsent(ipAddress, k -> new CounterEntry());
        long windowCount = windowEntry.incrementAndGet();

        // 增加每日计数
        AtomicLong dailyCountAtom = dailyCounter.computeIfAbsent(ipAddress, k -> new AtomicLong(0));
        long dailyCount = dailyCountAtom.incrementAndGet();

        int maxWindowRequests = ConfigConstants.getMonitorFrequencyMaxRequests();
        int maxDailyRequests = ConfigConstants.getMonitorDailyMaxRequests();

        boolean overWindowLimit = windowCount >= maxWindowRequests;
        boolean overDailyLimit = dailyCount >= maxDailyRequests;
        boolean blocked = overWindowLimit || overDailyLimit;

        String reason = null;
        if (blocked) {
            if (overWindowLimit && overDailyLimit) {
                reason = String.format("频率超限: %d分钟内请求%d次(阈值:%d), 今日请求%d次(阈值:%d)",
                        ConfigConstants.getMonitorFrequencyWindowMinutes(), windowCount, maxWindowRequests,
                        dailyCount, maxDailyRequests);
            } else if (overWindowLimit) {
                reason = String.format("频率超限: %d分钟内请求%d次, 阈值: %d",
                        ConfigConstants.getMonitorFrequencyWindowMinutes(), windowCount, maxWindowRequests);
            } else {
                reason = String.format("每日请求超限: %d次, 阈值: %d", dailyCount, maxDailyRequests);
            }
        }

        return new LimitCheckResult(overWindowLimit, overDailyLimit, windowCount, dailyCount, reason);
    }

    /**
     * 限流检查结果
     */
    public static class LimitCheckResult {
        private final boolean overWindowLimit;
        private final boolean overDailyLimit;
        private final long windowCount;
        private final long dailyCount;
        private final String blockReason;

        public LimitCheckResult(boolean overWindowLimit, boolean overDailyLimit,
                                long windowCount, long dailyCount,
                                String blockReason) {
            this.overWindowLimit = overWindowLimit;
            this.overDailyLimit = overDailyLimit;
            this.windowCount = windowCount;
            this.dailyCount = dailyCount;
            this.blockReason = blockReason;
        }

        public boolean isBlocked() {
            return overWindowLimit || overDailyLimit;
        }

        public String getBlockReason() {
            return blockReason != null ? blockReason : "访问被限制";
        }

        public boolean isOverDailyLimit() {
            return overDailyLimit;
        }

        public long getWindowCount() {
            return windowCount;
        }

        public long getDailyCount() {
            return dailyCount;
        }
    }
}