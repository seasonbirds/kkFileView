package cn.keking.service;

import org.redisson.api.RBucket;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 访问计数缓存服务
 * @author: system
 * @since: 2024/01/01
 */
@Service
@ConditionalOnExpression("'${user.behavior.cache.type:jdk}'.equals('redis')")
public class AccessCountCacheService {

    private static final Logger logger = LoggerFactory.getLogger(AccessCountCacheService.class);

    @Autowired(required = false)
    @Qualifier("userBehaviorRedissonClient")
    private RedissonClient redissonClient;

    /**
     * 增加IP在时间窗口内的访问计数
     */
    public void incrementTimeWindowCount(String ipAddress, int timeWindowMinutes) {
        if (redissonClient == null) {
            logger.debug("Redis未配置，跳过缓存操作");
            return;
        }

        try {
            String key = "access_count:time_window:" + ipAddress;
            RBucket<Long> bucket = redissonClient.getBucket(key);
            Long count = bucket.get();
            if (count == null) {
                count = 0L;
            }
            bucket.set(count + 1, timeWindowMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            logger.error("增加时间窗口访问计数时发生错误", e);
        }
    }

    /**
     * 获取IP在时间窗口内的访问计数
     */
    public long getTimeWindowCount(String ipAddress) {
        if (redissonClient == null) {
            logger.debug("Redis未配置，返回0");
            return 0;
        }

        try {
            String key = "access_count:time_window:" + ipAddress;
            RBucket<Long> bucket = redissonClient.getBucket(key);
            Long count = bucket.get();
            return count == null ? 0 : count;
        } catch (Exception e) {
            logger.error("获取时间窗口访问计数时发生错误", e);
            return 0;
        }
    }

    /**
     * 增加IP在当天的访问计数
     */
    public void incrementDailyCount(String ipAddress) {
        if (redissonClient == null) {
            logger.debug("Redis未配置，跳过缓存操作");
            return;
        }

        try {
            // 使用日期作为键的一部分，确保每天重置计数
            String dateKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String key = "access_count:daily:" + ipAddress + ":" + dateKey;
            RBucket<Long> bucket = redissonClient.getBucket(key);
            Long count = bucket.get();
            if (count == null) {
                count = 0L;
            }
            bucket.set(count + 1, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            logger.error("增加每日访问计数时发生错误", e);
        }
    }

    /**
     * 获取IP在当天的访问计数
     */
    public long getDailyCount(String ipAddress) {
        if (redissonClient == null) {
            logger.debug("Redis未配置，返回0");
            return 0;
        }

        try {
            String dateKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String key = "access_count:daily:" + ipAddress + ":" + dateKey;
            RBucket<Long> bucket = redissonClient.getBucket(key);
            Long count = bucket.get();
            return count == null ? 0 : count;
        } catch (Exception e) {
            logger.error("获取每日访问计数时发生错误", e);
            return 0;
        }
    }

    /**
     * 检查Redis是否可用
     */
    public boolean isRedisAvailable() {
        return redissonClient != null;
    }
}