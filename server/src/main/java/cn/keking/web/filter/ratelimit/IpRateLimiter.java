package cn.keking.web.filter.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IP限流器
 * 基于IP地址的固定窗口限流算法
 * 
 * 限流逻辑：
 * 1. 每个IP有一个时间窗口（m秒）
 * 2. 在时间窗口内，最多允许访问n次
 * 3. 时间窗口结束后，计数重置，开始新的窗口
 * 
 * 线程安全：使用ConcurrentHashMap.compute()保证原子操作
 * 
 * 使用RateLimitCache接口存储数据，通过RateLimitCacheFactory获取缓存实例
 * 便于后续扩展不同的缓存实现
 */
public class IpRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(IpRateLimiter.class);

    private final RateLimitCache cache;
    private final int windowSeconds;
    private final int maxRequests;

    public IpRateLimiter(int windowSeconds, int maxRequests) {
        this.cache = RateLimitCacheFactory.getInstance();
        this.windowSeconds = windowSeconds;
        this.maxRequests = maxRequests;
        logger.info("IpRateLimiter initialized: window={}s, maxRequests={}", windowSeconds, maxRequests);
    }

    public IpRateLimiter(RateLimitCache cache, int windowSeconds, int maxRequests) {
        this.cache = cache;
        this.windowSeconds = windowSeconds;
        this.maxRequests = maxRequests;
        logger.info("IpRateLimiter initialized with custom cache: window={}s, maxRequests={}", windowSeconds, maxRequests);
    }

    /**
     * 检查是否允许访问
     * 线程安全：使用cache.compute()保证整个操作的原子性
     * 
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
            long windowMillis = windowSeconds * 1000L;

            boolean[] allowed = {true};

            cache.compute(ip, (key, data) -> {
                if (data == null) {
                    allowed[0] = true;
                    logger.debug("New IP {}: count=1, windowStart={}", ip, now);
                    return new RateLimitData(1, now);
                }

                long windowStartTime = data.getWindowStartTime();
                long windowEndTime = windowStartTime + windowMillis;

                if (now > windowEndTime) {
                    data.setCount(1);
                    data.setWindowStartTime(now);
                    allowed[0] = true;
                    logger.debug("IP {}: window expired, reset count=1, new windowStart={}", ip, now);
                    return data;
                }

                int currentCount = data.getCount();
                if (currentCount >= maxRequests) {
                    allowed[0] = false;
                    logger.warn("IP {}: rate limit exceeded! current={}, max={}, windowEnd={}",
                            ip, currentCount, maxRequests, windowEndTime);
                    return data;
                }

                data.incrementAndGet();
                allowed[0] = true;
                logger.debug("IP {}: request allowed, count={}/{}", ip, data.getCount(), maxRequests);
                return data;
            });

            return allowed[0];

        } catch (Exception e) {
            logger.error("Error checking rate limit for IP: {}, allowing access due to exception", ip, e);
            return true;
        }
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public int getMaxRequests() {
        return maxRequests;
    }
}
