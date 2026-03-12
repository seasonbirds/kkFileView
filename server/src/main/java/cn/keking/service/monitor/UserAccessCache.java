package cn.keking.service.monitor;

import cn.keking.config.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * 用户访问统计本地缓存
 * 用于提高访问统计性能，避免实时查询数据库
 */
@Component
public class UserAccessCache {
    private static final Logger logger = LoggerFactory.getLogger(UserAccessCache.class);

    /**
     * IP访问时间窗口统计：key=IP地址，value=时间窗口内的访问时间戳列表
     */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Long>> ipAccessWindow = new ConcurrentHashMap<>();

    /**
     * IP每日访问统计：key=IP地址，value=当日访问次数
     */
    private final ConcurrentHashMap<String, Integer> ipDailyCount = new ConcurrentHashMap<>();

    /**
     * 记录访问
     *
     * @param ipAddress IP地址
     */
    public void recordAccess(String ipAddress) {
        if (!ConfigConstants.isBehaviorMonitorEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();

        // 更新时间窗口统计
        ipAccessWindow.compute(ipAddress, (ip, timestamps) -> {
            if (timestamps == null) {
                timestamps = new CopyOnWriteArrayList<>();
            }
            timestamps.add(now);
            return timestamps;
        });

        // 更新每日统计
        ipDailyCount.merge(ipAddress, 1, Integer::sum);
    }

    /**
     * 判断IP是否在时间窗口内超过请求阈值
     *
     * @param ipAddress IP地址
     * @return true=超过阈值，false=未超过
     */
    public boolean isRateLimited(String ipAddress) {
        if (!ConfigConstants.isBehaviorMonitorEnabled()) {
            return false;
        }

        CopyOnWriteArrayList<Long> timestamps = ipAccessWindow.get(ipAddress);
        if (timestamps == null || timestamps.isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();
        int timeWindow = ConfigConstants.getBehaviorMonitorTimeWindow();
        long windowStart = now - TimeUnit.MINUTES.toMillis(timeWindow);

        // 清理过期的时间戳
        timestamps.removeIf(timestamp -> timestamp < windowStart);

        return timestamps.size() >= ConfigConstants.getBehaviorMonitorMaxRequests();
    }

    /**
     * 判断IP是否超过每日请求阈值
     *
     * @param ipAddress IP地址
     * @return true=超过阈值，false=未超过
     */
    public boolean isDailyLimitExceeded(String ipAddress) {
        if (!ConfigConstants.isBehaviorMonitorEnabled()) {
            return false;
        }

        Integer count = ipDailyCount.get(ipAddress);
        return count != null && count >= ConfigConstants.getBehaviorMonitorDailyMaxRequests();
    }

    /**
     * 获取IP在时间窗口内的请求次数
     *
     * @param ipAddress IP地址
     * @return 请求次数
     */
    public int getWindowRequestCount(String ipAddress) {
        CopyOnWriteArrayList<Long> timestamps = ipAccessWindow.get(ipAddress);
        if (timestamps == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        int timeWindow = ConfigConstants.getBehaviorMonitorTimeWindow();
        long windowStart = now - TimeUnit.MINUTES.toMillis(timeWindow);

        return (int) timestamps.stream().filter(timestamp -> timestamp >= windowStart).count();
    }

    /**
     * 获取IP当日请求次数
     *
     * @param ipAddress IP地址
     * @return 请求次数
     */
    public int getDailyRequestCount(String ipAddress) {
        return ipDailyCount.getOrDefault(ipAddress, 0);
    }

    /**
     * 每日凌晨清理每日统计数据
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void clearDailyCache() {
        ipDailyCount.clear();
        logger.info("Daily access cache cleared");
    }

    /**
     * 每小时清理过期的时间窗口数据
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void clearExpiredWindowData() {
        long now = System.currentTimeMillis();
        int timeWindow = ConfigConstants.getBehaviorMonitorTimeWindow();
        long windowStart = now - TimeUnit.MINUTES.toMillis(timeWindow);

        for (Map.Entry<String, CopyOnWriteArrayList<Long>> entry : ipAccessWindow.entrySet()) {
            CopyOnWriteArrayList<Long> timestamps = entry.getValue();
            timestamps.removeIf(timestamp -> timestamp < windowStart);
            if (timestamps.isEmpty()) {
                ipAccessWindow.remove(entry.getKey());
            }
        }
        logger.info("Expired window data cleared");
    }
}
