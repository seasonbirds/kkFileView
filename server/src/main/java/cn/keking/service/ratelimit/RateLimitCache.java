package cn.keking.service.ratelimit;

/**
 * 限流缓存接口，用于检查请求是否被允许
 * 提供统一的接口，便于后续扩展支持不同的缓存实现（如Redis）
 *
 * @author kkfileview
 * @since 2026-04-04
 */
public interface RateLimitCache {

    /**
     * 检查指定key的请求是否被允许
     *
     * @param key          限流的key（如IP地址）
     * @param maxRequests  周期内的最大请求次数
     * @param periodSeconds 周期时间（秒）
     * @return true表示请求被允许，false表示请求被限流
     */
    boolean isAllowed(String key, int maxRequests, int periodSeconds);
}
