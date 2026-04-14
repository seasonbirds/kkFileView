package cn.keking.web.filter.ratelimit;

/**
 * 限流缓存接口
 * 定义限流数据的存储和访问操作
 * 采用接口设计，便于后续扩展不同的缓存实现（如Redis）
 */
public interface RateLimitCache {

    /**
     * 获取指定IP的访问计数
     * @param ip IP地址
     * @return 访问计数，如果不存在则返回null
     */
    Integer getCount(String ip);

    /**
     * 设置指定IP的访问计数
     * @param ip IP地址
     * @param count 访问计数
     * @param windowSeconds 时间窗口（秒），用于设置过期时间
     */
    void setCount(String ip, int count, int windowSeconds);

    /**
     * 增加指定IP的访问计数
     * @param ip IP地址
     * @param windowSeconds 时间窗口（秒）
     * @return 增加后的计数
     */
    int incrementAndGet(String ip, int windowSeconds);

    /**
     * 获取指定IP的窗口开始时间
     * @param ip IP地址
     * @return 窗口开始时间戳（毫秒），如果不存在则返回null
     */
    Long getWindowStartTime(String ip);

    /**
     * 设置指定IP的窗口开始时间
     * @param ip IP地址
     * @param startTime 窗口开始时间戳（毫秒）
     * @param windowSeconds 时间窗口（秒）
     */
    void setWindowStartTime(String ip, long startTime, int windowSeconds);

    /**
     * 重置指定IP的限流数据
     * @param ip IP地址
     */
    void reset(String ip);

    /**
     * 清理过期的限流数据
     * 用于定期清理不再活跃的IP数据，释放内存
     */
    void cleanExpired();
}
