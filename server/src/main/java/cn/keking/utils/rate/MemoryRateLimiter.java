package cn.keking.utils.rate;

import cn.keking.config.ConfigConstants;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存的限流器实现
 */
public class MemoryRateLimiter implements RateLimiter {

    private final ConcurrentMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    private int period;
    private int limit;

    public MemoryRateLimiter() {
        // 从ConfigConstants获取默认值
        this.period = ConfigConstants.getRateLimitPeriod();
        this.limit = ConfigConstants.getRateLimitCount();
    }

    @Override
    public boolean allow(String key) {
        try {
            long now = System.currentTimeMillis();
            RateLimitInfo info = rateLimitMap.get(key);
            if (info == null) {
                // 首次访问，创建新记录
                RateLimitInfo newInfo = new RateLimitInfo(now, 1);
                rateLimitMap.put(key, newInfo);
                return true;
            } else {
                // 检查是否在周期内
                if (now - info.getStartTime() > period) {
                    // 超过周期，重置计数
                    info.setStartTime(now);
                    info.setCount(1);
                    return true;
                } else {
                    // 在周期内，检查是否超过限制
                    if (info.getCount() >= limit) {
                        return false;
                    } else {
                        // 增加计数
                        info.incrementCount();
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 异常时允许访问，避免影响正常功能
            return true;
        }
    }

    @Override
    public void setLimit(int period, int limit) {
        this.period = period;
        this.limit = limit;
    }

    /**
     * 限流信息类
     */
    private static class RateLimitInfo {
        private final AtomicLong startTime;
        private final AtomicInteger count;

        public RateLimitInfo(long startTime, int count) {
            this.startTime = new AtomicLong(startTime);
            this.count = new AtomicInteger(count);
        }

        public long getStartTime() {
            return startTime.get();
        }

        public void setStartTime(long startTime) {
            this.startTime.set(startTime);
        }

        public int getCount() {
            return count.get();
        }

        public void setCount(int count) {
            this.count.set(count);
        }

        public void incrementCount() {
            this.count.incrementAndGet();
        }
    }
}
