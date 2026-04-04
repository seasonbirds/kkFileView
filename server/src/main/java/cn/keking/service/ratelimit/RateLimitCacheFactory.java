package cn.keking.service.ratelimit;

import cn.keking.service.ratelimit.impl.RateLimitCacheMapImpl;
import org.springframework.stereotype.Component;

/**
 * 限流缓存工厂类，使用工厂模式创建限流缓存实例
 * 便于后续扩展支持不同的缓存实现（如Redis）
 *
 * @author kkfileview
 * @since 2026-04-04
 */
@Component
public class RateLimitCacheFactory {

    /**
     * 获取限流缓存实例
     * 当前默认使用基于Map的实现
     *
     * @return 限流缓存实例
     */
    public RateLimitCache getRateLimitCache() {
        return new RateLimitCacheMapImpl();
    }
}
