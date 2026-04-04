package cn.keking.service.ratelimit.impl;

import cn.keking.service.ratelimit.RateLimitCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于Map的限流缓存实现
 * 使用ConcurrentHashMap保证线程安全
 * 适用于单机部署场景
 *
 * @author kkfileview
 * @since 2026-04-04
 */
public class RateLimitCacheMapImpl implements RateLimitCache {

    private final Map<String, RateLimitInfo> cache = new ConcurrentHashMap<>();

    /**
     * 检查指定key的请求是否被允许
     * 使用滑动窗口算法，每个周期内重新计数
     *
     * @param key          限流的key（如IP地址）
     * @param maxRequests  周期内的最大请求次数
     * @param periodSeconds 周期时间（秒）
     * @return true表示请求被允许，false表示请求被限流
     */
    @Override
    public boolean isAllowed(String key, int maxRequests, int periodSeconds) {
        long currentTime = System.currentTimeMillis();
        long periodMs = TimeUnit.SECONDS.toMillis(periodSeconds);

        RateLimitInfo info = cache.get(key);
        if (info == null || currentTime - info.startTime > periodMs) {
            info = new RateLimitInfo(currentTime, 1);
            cache.put(key, info);
            return true;
        }

        if (info.count < maxRequests) {
            info.count++;
            return true;
        }

        return false;
    }

    /**
     * 限流信息内部类
     * 记录每个key的开始时间和请求次数
     */
    private static class RateLimitInfo {
        long startTime;
        int count;

        RateLimitInfo(long startTime, int count) {
            this.startTime = startTime;
            this.count = count;
        }
    }
}
