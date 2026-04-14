package cn.keking.web.filter.ratelimit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流数据
 * 用于存储每个IP的访问计数和时间窗口信息
 * 线程安全：使用原子类保证并发安全
 */
public class RateLimitData {

    private final AtomicInteger count;
    private final AtomicLong windowStartTime;

    public RateLimitData(int count, long windowStartTime) {
        this.count = new AtomicInteger(count);
        this.windowStartTime = new AtomicLong(windowStartTime);
    }

    public int getCount() {
        return count.get();
    }

    public void setCount(int count) {
        this.count.set(count);
    }

    public int incrementAndGet() {
        return count.incrementAndGet();
    }

    public long getWindowStartTime() {
        return windowStartTime.get();
    }

    public void setWindowStartTime(long windowStartTime) {
        this.windowStartTime.set(windowStartTime);
    }

    public boolean compareAndSetWindowStartTime(long expect, long update) {
        return windowStartTime.compareAndSet(expect, update);
    }
}
