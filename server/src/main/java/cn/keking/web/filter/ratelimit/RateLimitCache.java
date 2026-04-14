package cn.keking.web.filter.ratelimit;

import java.util.function.BiFunction;

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

    /**
     * 原子操作：如果IP不存在则设置，否则返回已存在的值
     * @param ip IP地址
     * @param data 要设置的限流数据
     * @return 已存在的限流数据，如果不存在则返回null
     */
    RateLimitData putIfAbsent(String ip, RateLimitData data);

    /**
     * 原子操作：对指定IP的限流数据进行计算
     * 整个计算过程是原子的，保证线程安全
     * @param ip IP地址
     * @param remappingFunction 计算函数，参数为(ip, 旧数据)，返回新数据
     * @return 计算后的新数据
     */
    RateLimitData compute(String ip, BiFunction<String, RateLimitData, RateLimitData> remappingFunction);
}
