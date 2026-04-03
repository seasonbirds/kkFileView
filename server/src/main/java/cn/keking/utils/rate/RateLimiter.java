package cn.keking.utils.rate;

/**
 * 限流器接口
 */
public interface RateLimiter {

    /**
     * 检查是否允许访问
     * @param key 限流键，如IP地址
     * @return true表示允许访问，false表示限流
     */
    boolean allow(String key);

    /**
     * 设置限流参数
     * @param period 周期（毫秒）
     * @param limit 周期内限制次数
     */
    void setLimit(int period, int limit);
}
