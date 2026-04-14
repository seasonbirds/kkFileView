package cn.keking.web.filter.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 限流缓存工厂
 * 使用工厂模式创建限流缓存实例
 * 便于后续扩展支持不同的缓存实现
 * 
 * 扩展方式：
 * 1. 创建新的缓存实现类，实现RateLimitCache接口
 * 2. 在getInstance()方法中添加新的类型判断
 * 3. 在配置中指定缓存类型
 */
public class RateLimitCacheFactory {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitCacheFactory.class);

    public static final String TYPE_LOCAL = "local";

    private static volatile RateLimitCache instance;

    private RateLimitCacheFactory() {
    }

    /**
     * 获取默认的限流缓存实例（单例）
     * 默认使用本地Map实现
     * @return 限流缓存实例
     */
    public static RateLimitCache getInstance() {
        return getInstance(TYPE_LOCAL);
    }

    /**
     * 根据类型获取限流缓存实例（单例）
     * @param cacheType 缓存类型
     * @return 限流缓存实例
     */
    public static RateLimitCache getInstance(String cacheType) {
        if (instance == null) {
            synchronized (RateLimitCacheFactory.class) {
                if (instance == null) {
                    instance = createCache(cacheType);
                }
            }
        }
        return instance;
    }

    /**
     * 创建限流缓存实例
     * @param cacheType 缓存类型
     * @return 限流缓存实例
     */
    private static RateLimitCache createCache(String cacheType) {
        logger.info("Creating rate limit cache with type: {}", cacheType);
        return new LocalMapRateLimitCache();
    }

    /**
     * 重置缓存实例
     * 主要用于测试场景
     */
    public static void reset() {
        instance = null;
        logger.info("RateLimitCacheFactory reset");
    }
}
