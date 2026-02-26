package cn.keking.behavior.cache;

import cn.keking.behavior.config.BehaviorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户行为本地缓存
 * 用于存储IP访问计数，避免实时查询数据库影响性能
 */
@Component
public class BehaviorCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(BehaviorCacheManager.class);

    /**
     * 周期内IP访问计数缓存
     * Key: IP地址
     * Value: 访问次数
     */
    private final Map<String, AtomicInteger> periodCountCache = new ConcurrentHashMap<>();

    /**
     * 每日IP访问计数缓存
     * Key: IP地址
     * Value: 访问次数
     */
    private final Map<String, AtomicInteger> dailyCountCache = new ConcurrentHashMap<>();

    /**
     * 当前统计周期开始时间
     */
    private volatile LocalDateTime periodStartTime = LocalDateTime.now();

    /**
     * 当前统计日期
     */
    private volatile LocalDate currentDate = LocalDate.now();

    /**
     * 周期内已发送告警的IP集合
     */
    private final Map<String, Boolean> periodAlertSentCache = new ConcurrentHashMap<>();

    /**
     * 每日已发送告警的IP集合
     */
    private final Map<String, Boolean> dailyAlertSentCache = new ConcurrentHashMap<>();

    /**
     * 周期计数器的锁对象
     * 用于避免周期计数重置时的并发问题
     */
    private final Object periodLock = new Object();

    /**
     * 每日计数器的锁对象
     * 用于避免每日计数重置时的并发问题
     */
    private final Object dailyLock = new Object();

    /**
     * 增加IP的周期内访问计数
     * @param ipAddress IP地址
     * @return 增加后的计数
     */
    public int incrementPeriodCount(String ipAddress) {
        checkAndResetPeriod();
        AtomicInteger count = periodCountCache.computeIfAbsent(ipAddress, k -> new AtomicInteger(0));
        return count.incrementAndGet();
    }

    /**
     * 增加IP的每日访问计数
     * @param ipAddress IP地址
     * @return 增加后的计数
     */
    public int incrementDailyCount(String ipAddress) {
        checkAndResetDaily();
        AtomicInteger count = dailyCountCache.computeIfAbsent(ipAddress, k -> new AtomicInteger(0));
        return count.incrementAndGet();
    }

    /**
     * 获取IP的周期内访问计数
     * @param ipAddress IP地址
     * @return 访问计数
     */
    public int getPeriodCount(String ipAddress) {
        checkAndResetPeriod();
        AtomicInteger count = periodCountCache.get(ipAddress);
        return count == null ? 0 : count.get();
    }

    /**
     * 获取IP的每日访问计数
     * @param ipAddress IP地址
     * @return 访问计数
     */
    public int getDailyCount(String ipAddress) {
        checkAndResetDaily();
        AtomicInteger count = dailyCountCache.get(ipAddress);
        return count == null ? 0 : count.get();
    }

    /**
     * 检查周期内访问是否超过阈值
     * @param ipAddress IP地址
     * @return 是否超过阈值
     */
    public boolean isPeriodThresholdExceeded(String ipAddress) {
        return getPeriodCount(ipAddress) > BehaviorConfig.getPeriodThreshold();
    }

    /**
     * 检查每日访问是否超过阈值
     * @param ipAddress IP地址
     * @return 是否超过阈值
     */
    public boolean isDailyThresholdExceeded(String ipAddress) {
        return getDailyCount(ipAddress) > BehaviorConfig.getDailyThreshold();
    }

    /**
     * 检查是否应该发送周期告警（每个周期每个IP只发送一次）
     * @param ipAddress IP地址
     * @return 是否应该发送
     */
    public boolean shouldSendPeriodAlert(String ipAddress) {
        return periodAlertSentCache.putIfAbsent(ipAddress, true) == null;
    }

    /**
     * 检查是否应该发送每日告警（每天每个IP只发送一次）
     * @param ipAddress IP地址
     * @return 是否应该发送
     */
    public boolean shouldSendDailyAlert(String ipAddress) {
        return dailyAlertSentCache.putIfAbsent(ipAddress, true) == null;
    }

    /**
     * 检查并重置周期计数器
     * 使用独立的periodLock，避免与每日计数器产生锁竞争
     */
    private void checkAndResetPeriod() {
        LocalDateTime now = LocalDateTime.now();
        int periodMinutes = BehaviorConfig.getPeriodMinutes();
        if (now.isAfter(periodStartTime.plusMinutes(periodMinutes))) {
            synchronized (periodLock) {
                if (now.isAfter(periodStartTime.plusMinutes(periodMinutes))) {
                    periodStartTime = now;
                    periodCountCache.clear();
                    periodAlertSentCache.clear();
                    logger.debug("Period cache reset at {}", now);
                }
            }
        }
    }

    /**
     * 检查并重置每日计数器
     * 使用独立的dailyLock，避免与周期计数器产生锁竞争
     */
    private void checkAndResetDaily() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            synchronized (dailyLock) {
                if (!today.equals(currentDate)) {
                    currentDate = today;
                    dailyCountCache.clear();
                    dailyAlertSentCache.clear();
                    logger.debug("Daily cache reset at {}", today);
                }
            }
        }
    }

    /**
     * 获取周期开始时间
     * @return 周期开始时间
     */
    public LocalDateTime getPeriodStartTime() {
        return periodStartTime;
    }

    /**
     * 获取当前日期
     * @return 当前日期
     */
    public LocalDate getCurrentDate() {
        return currentDate;
    }
}
