package cn.keking.web.filter.ratelimit;

/**
 * 限流数据
 * 用于存储每个IP的访问计数和时间窗口信息
 */
public class RateLimitData {

    private int count;
    private long windowStartTime;

    public RateLimitData(int count, long windowStartTime) {
        this.count = count;
        this.windowStartTime = windowStartTime;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incrementCount() {
        this.count++;
    }

    public long getWindowStartTime() {
        return windowStartTime;
    }

    public void setWindowStartTime(long windowStartTime) {
        this.windowStartTime = windowStartTime;
    }
}
