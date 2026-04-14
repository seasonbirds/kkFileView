package cn.keking.web.filter.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * IP限流器
 * 基于IP地址的固定窗口限流算法
 * 
 * 限流逻辑：
 * 1. 每个IP有一个时间窗口（m秒）
 * 2. 在时间窗口内，最多允许访问n次
 * 3. 时间窗口结束后，计数重置，开始新的窗口
 */
public class IpRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(IpRateLimiter.class);

    private final ConcurrentHashMap<String, RateLimitData> ipDataMap = new ConcurrentHashMap<>();

    private final int windowSeconds;
    private final int maxRequests;

    public IpRateLimiter(int windowSeconds, int maxRequests) {
        this.windowSeconds = windowSeconds;
        this.maxRequests = maxRequests;
        logger.info("IpRateLimiter initialized: window={}s, maxRequests={}", windowSeconds, maxRequests);
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
            long windowMillis = windowSeconds * 1000L;

            RateLimitData data = ipDataMap.get(ip);

            if (data == null) {
                data = new RateLimitData(1, now);
                ipDataMap.put(ip, data);
                logger.debug("New IP {}: count=1, windowStart={}", ip, now);
                return true;
            }

            long windowEndTime = data.getWindowStartTime() + windowMillis;

            if (now > windowEndTime) {
                data.setCount(1);
                data.setWindowStartTime(now);
                logger.debug("IP {}: window expired, reset count=1, new windowStart={}", ip, now);
                return true;
            }

            if (data.getCount() >= maxRequests) {
                logger.warn("IP {}: rate limit exceeded! current={}, max={}, windowEnd={}",
                        ip, data.getCount(), maxRequests, windowEndTime);
                return false;
            }

            data.incrementCount();
            logger.debug("IP {}: request allowed, count={}/{}", ip, data.getCount(), maxRequests);
            return true;

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
