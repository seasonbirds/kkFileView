package cn.keking.web.filter.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 限流服务
 * 核心限流逻辑实现
 * 基于IP地址的滑动窗口限流算法
 */
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    private final RateLimitCache cache;
    private final int windowSeconds;
    private final int maxRequests;

    public RateLimitService(RateLimitCache cache, int windowSeconds, int maxRequests) {
        this.cache = cache;
        this.windowSeconds = windowSeconds;
        this.maxRequests = maxRequests;
        logger.info("RateLimitService initialized: window={}s, maxRequests={}", windowSeconds, maxRequests);
    }

    /**
     * 检查是否允许访问
     * @param ip IP地址
     * @return true-允许访问，false-超出限制
     */
    public boolean isAllowed(String ip) {
        try {
            if (ip == null || ip.isEmpty()) {
                logger.warn("IP address is null or empty, allowing access");
                return true;
            }

            long now = System.currentTimeMillis();
            Long windowStartTime = cache.getWindowStartTime(ip);

            if (windowStartTime == null) {
                cache.setWindowStartTime(ip, now, windowSeconds);
                cache.setCount(ip, 1, windowSeconds);
                logger.debug("New IP {}: window started at {}, count=1", ip, now);
                return true;
            }

            long windowEndTime = windowStartTime + windowSeconds * 1000L;

            if (now > windowEndTime) {
                cache.reset(ip);
                cache.setWindowStartTime(ip, now, windowSeconds);
                cache.setCount(ip, 1, windowSeconds);
                logger.debug("IP {}: window expired, new window started at {}, count=1", ip, now);
                return true;
            }

            Integer currentCount = cache.getCount(ip);
            if (currentCount == null) {
                currentCount = 0;
            }

            if (currentCount >= maxRequests) {
                logger.warn("IP {}: rate limit exceeded! current={}, max={}, windowEnd={}",
                        ip, currentCount, maxRequests, windowEndTime);
                return false;
            }

            int newCount = cache.incrementAndGet(ip, windowSeconds);
            logger.debug("IP {}: request allowed, count={}/{}", ip, newCount, maxRequests);
            return true;

        } catch (Exception e) {
            logger.error("Error checking rate limit for IP: {}, allowing access due to exception", ip, e);
            return true;
        }
    }

    /**
     * 获取当前IP的访问计数
     * @param ip IP地址
     * @return 访问计数
     */
    public int getCurrentCount(String ip) {
        try {
            Integer count = cache.getCount(ip);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Error getting current count for IP: {}", ip, e);
            return 0;
        }
    }

    /**
     * 重置指定IP的限流数据
     * @param ip IP地址
     */
    public void reset(String ip) {
        try {
            cache.reset(ip);
            logger.debug("Rate limit reset for IP: {}", ip);
        } catch (Exception e) {
            logger.error("Error resetting rate limit for IP: {}", ip, e);
        }
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public int getMaxRequests() {
        return maxRequests;
    }
}
