package cn.keking.web.filter.ratelimit;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于本地ConcurrentHashMap的限流缓存实现
 * 使用Java原生实现，不依赖第三方库
 * 支持线程安全的并发访问
 */
public class LocalMapRateLimitCache implements RateLimitCache {

    private final ConcurrentHashMap<String, RateLimitData> ipDataMap = new ConcurrentHashMap<>();

    @Override
    public RateLimitData get(String ip) {
        return ipDataMap.get(ip);
    }

    @Override
    public void set(String ip, RateLimitData data) {
        ipDataMap.put(ip, data);
    }

    @Override
    public void remove(String ip) {
        ipDataMap.remove(ip);
    }
}
