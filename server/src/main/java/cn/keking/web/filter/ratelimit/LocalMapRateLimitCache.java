package cn.keking.web.filter.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基于本地ConcurrentHashMap的限流缓存实现
 * 使用Java原生实现，不依赖第三方库
 * 支持线程安全的并发访问
 */
public class LocalMapRateLimitCache implements RateLimitCache {

    private static final Logger logger = LoggerFactory.getLogger(LocalMapRateLimitCache.class);

    private final Map<String, Integer> countMap = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStartTimeMap = new ConcurrentHashMap<>();
    private final Map<String, Long> expireTimeMap = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = new Thread(r, "rate-limit-cache-cleaner");
                t.setDaemon(true);
                return t;
            }
    );

    public LocalMapRateLimitCache() {
        scheduler.scheduleAtFixedRate(this::cleanExpired, 60, 60, TimeUnit.SECONDS);
        logger.info("LocalMapRateLimitCache initialized with auto-clean every 60 seconds");
    }

    @Override
    public Integer getCount(String ip) {
        try {
            return countMap.get(ip);
        } catch (Exception e) {
            logger.error("Error getting count for IP: {}", ip, e);
            return null;
        }
    }

    @Override
    public void setCount(String ip, int count, int windowSeconds) {
        try {
            countMap.put(ip, count);
            expireTimeMap.put(ip, System.currentTimeMillis() + windowSeconds * 1000L);
        } catch (Exception e) {
            logger.error("Error setting count for IP: {}", ip, e);
        }
    }

    @Override
    public int incrementAndGet(String ip, int windowSeconds) {
        try {
            return countMap.merge(ip, 1, Integer::sum);
        } catch (Exception e) {
            logger.error("Error incrementing count for IP: {}", ip, e);
            return 1;
        }
    }

    @Override
    public Long getWindowStartTime(String ip) {
        try {
            return windowStartTimeMap.get(ip);
        } catch (Exception e) {
            logger.error("Error getting window start time for IP: {}", ip, e);
            return null;
        }
    }

    @Override
    public void setWindowStartTime(String ip, long startTime, int windowSeconds) {
        try {
            windowStartTimeMap.put(ip, startTime);
            expireTimeMap.put(ip, startTime + windowSeconds * 1000L);
        } catch (Exception e) {
            logger.error("Error setting window start time for IP: {}", ip, e);
        }
    }

    @Override
    public void reset(String ip) {
        try {
            countMap.remove(ip);
            windowStartTimeMap.remove(ip);
            expireTimeMap.remove(ip);
        } catch (Exception e) {
            logger.error("Error resetting data for IP: {}", ip, e);
        }
    }

    @Override
    public void cleanExpired() {
        try {
            long now = System.currentTimeMillis();
            int cleanedCount = 0;

            for (Map.Entry<String, Long> entry : expireTimeMap.entrySet()) {
                String ip = entry.getKey();
                Long expireTime = entry.getValue();
                if (expireTime != null && now > expireTime) {
                    reset(ip);
                    cleanedCount++;
                }
            }

            if (cleanedCount > 0) {
                logger.debug("Cleaned {} expired rate limit entries", cleanedCount);
            }
        } catch (Exception e) {
            logger.error("Error cleaning expired entries", e);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("LocalMapRateLimitCache scheduler shutdown");
    }
}
