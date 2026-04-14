package cn.keking.web.filter.ratelimit;

/**
 * 限流缓存接口
 * 定义限流数据的存储和访问操作
 * 采用接口设计，便于后续扩展不同的缓存实现
 */
public interface RateLimitCache {

    /**
     * 获取指定IP的限流数据
     * @param ip IP地址
     * @return 限流数据，如果不存在则返回null
     */
    RateLimitData get(String ip);

    /**
     * 设置指定IP的限流数据
     * @param ip IP地址
     * @param data 限流数据
     */
    void set(String ip, RateLimitData data);

    /**
     * 移除指定IP的限流数据
     * @param ip IP地址
     */
    void remove(String ip);
}
