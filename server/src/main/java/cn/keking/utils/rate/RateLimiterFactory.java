package cn.keking.utils.rate;

/**
 * 限流器工厂类
 */
public class RateLimiterFactory {

    private static final RateLimiterFactory INSTANCE = new RateLimiterFactory();
    private RateLimiter rateLimiter;

    private RateLimiterFactory() {
        // 默认使用内存限流器
        rateLimiter = new MemoryRateLimiter();
    }

    public static RateLimiterFactory getInstance() {
        return INSTANCE;
    }

    /**
     * 获取限流器实例
     * @return 限流器实例
     */
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    /**
     * 设置限流器实例（用于后续扩展，如Redis实现）
     * @param rateLimiter 限流器实例
     */
    public void setRateLimiter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }
}
