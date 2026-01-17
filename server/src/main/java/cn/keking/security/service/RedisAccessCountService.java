package cn.keking.security.service;

import cn.keking.security.config.BehaviorRedisConfig;
import cn.keking.security.config.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;

/**
 * Redis访问计数服务
 * 用于替代数据库查询，提升性能
 * 实现滑动窗口算法进行请求频率限制
 * 
 * <p>该服务提供以下核心功能：</p>
 * <ul>
 *     <li>周期请求计数（基于滑动窗口算法）</li>
 *     <li>每日请求计数（基于简单计数器）</li>
 *     <li>IP级别的访问统计和限流支持</li>
 *     <li>自动数据清理和过期管理</li>
 * </ul>
 * 
 * <p>Redis数据结构说明：</p>
 * <ul>
 *     <li>周期计数：使用ZSet存储每个请求的时间戳，支持滑动窗口查询</li>
 *     <li>每日计数：使用String存储计数值，设置24小时过期时间</li>
 * </ul>
 */
@Service
public class RedisAccessCountService {

    private static final Logger logger = LoggerFactory.getLogger(RedisAccessCountService.class);

    /**
     * 默认的每日计数过期时间（秒）
     * 24小时 = 86400秒
     */
    private static final int DAILY_COUNT_TTL = 86400;

    /**
     * 周期请求计数的Redis Key前缀
     * 格式: behavior:period:{ip}:{timestamp}
     */
    private static final String PERIOD_COUNT_KEY_PREFIX = "behavior:period:";

    /**
     * 每日请求计数的Redis Key前缀
     * 格式: behavior:daily:{ip}:{date}
     */
    private static final String DAILY_COUNT_KEY_PREFIX = "behavior:daily:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final BehaviorRedisConfig behaviorRedisConfig;
    private final SecurityConfig securityConfig;

    /**
     * Lua脚本：原子性地增加计数并获取当前周期内的总请求数
     * 使用滑动窗口算法实现精确的请求频率控制
     * 
     * <p>脚本逻辑：</p>
     * <ol>
     *     <li>计算滑动窗口的起始时间点（当前时间 - 窗口大小）</li>
     *     <li>使用ZADD命令将当前请求时间戳添加到有序集合中</li>
     *     <li>设置Key的过期时间为窗口大小，自动清理过期数据</li>
     *     <li>使用ZCOUNT命令统计窗口内的请求总数</li>
     * </ol>
     * 
     * <p>参数说明：</p>
     * <ul>
     *     <li>KEYS[1]: Redis Key前缀</li>
     *     <li>ARGV[1]: 用户IP地址</li>
     *     <li>ARGV[2]: 当前时间戳（毫秒）</li>
     *     <li>ARGV[3]: 滑动窗口大小（秒）</li>
     * </ul>
     */
    private static final String SLIDING_WINDOW_SCRIPT = """
        local keyPrefix = KEYS[1]
        local ip = ARGV[1]
        local currentTime = tonumber(ARGV[2])
        local windowSize = tonumber(ARGV[3])
        
        local windowStart = currentTime - windowSize * 1000
        local currentKey = keyPrefix .. ip .. ":" .. currentTime
        
        redis.call('ZADD', currentKey, currentTime, currentTime)
        redis.call('EXPIRE', currentKey, windowSize)
        
        local count = redis.call('ZCOUNT', currentKey, windowStart, currentTime)
        
        return count
    """;

    /**
     * 构造函数
     * 依赖注入Redis模板和配置对象
     * 
     * @param behaviorRedisTemplate Redis操作模板（已配置序列化方式）
     * @param behaviorRedisConfig Redis行为配置（开关、连接信息等）
     * @param securityConfig 安全配置（周期时间、限流阈值等）
     */
    public RedisAccessCountService(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            RedisTemplate<String, Object> behaviorRedisTemplate,
            BehaviorRedisConfig behaviorRedisConfig,
            SecurityConfig securityConfig) {
        this.redisTemplate = behaviorRedisTemplate;
        this.behaviorRedisConfig = behaviorRedisConfig;
        this.securityConfig = securityConfig;
    }

    /**
     * 检查并增加周期请求计数
     * 使用滑动窗口算法实现精确的请求频率控制
     * 
     * <p>实现原理：</p>
     * <ol>
     *     <li>检查Redis是否启用，未启用直接返回0</li>
     *     <li>获取当前时间戳和配置的窗口大小</li>
     *     <li>执行Lua脚本原子性地完成计数增加和查询</li>
     *     <li>返回当前周期内的总请求数</li>
     * </ol>
     * 
     * @param ipAddress 用户IP地址
     * @return 当前周期内的请求数，如果Redis未启用或执行失败返回0
     */
    public int incrementAndGetPeriodCount(String ipAddress) {
        if (!behaviorRedisConfig.isRedisEnabled()) {
            return 0;
        }

        try {
            long currentTime = System.currentTimeMillis();
            long windowSize = securityConfig.getPeriodMinutes() * 60 * 1000L;

            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptText(SLIDING_WINDOW_SCRIPT);
            script.setResultType(Long.class);

            Long result = redisTemplate.execute(
                    script,
                    Collections.singletonList(PERIOD_COUNT_KEY_PREFIX),
                    ipAddress,
                    String.valueOf(currentTime),
                    String.valueOf(windowSize)
            );

            return result != null ? result.intValue() : 0;
        } catch (Exception e) {
            logger.error("Failed to increment period count for IP: {}", ipAddress, e);
            return 0;
        }
    }

    /**
     * 检查并增加每日请求计数
     * 使用简单的计数器实现，Key格式为 behavior:daily:{ip}:{date}
     * 
     * <p>实现原理：</p>
     * <ol>
     *     <li>检查Redis是否启用，未启用直接返回0</li>
     *     <li>获取当前日期并构造Redis Key</li>
     *     <li>使用INCR命令原子性地增加计数</li>
     *     <li>如果是首次创建（count == 1），设置24小时过期时间</li>
     *     <li>返回当前日期的总请求数</li>
     * </ol>
     * 
     * @param ipAddress 用户IP地址
     * @return 当前日期的请求数，如果Redis未启用或执行失败返回0
     */
    public int incrementAndGetDailyCount(String ipAddress) {
        if (!behaviorRedisConfig.isRedisEnabled()) {
            return 0;
        }

        try {
            String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String key = DAILY_COUNT_KEY_PREFIX + ipAddress + ":" + date;

            Long count = redisTemplate.opsForValue().increment(key);

            if (count == 1) {
                redisTemplate.expire(key, DAILY_COUNT_TTL);
            }

            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            logger.error("Failed to increment daily count for IP: {}", ipAddress, e);
            return 0;
        }
    }

    /**
     * 获取当前周期内的请求数
     * 通过扫描匹配的Key并查询每个Key的窗口内数据
     * 
     * <p>实现原理：</p>
     * <ol>
     *     <li>计算滑动窗口的起始时间点</li>
     *     <li>使用KEYS命令匹配该IP的所有周期计数Key</li>
     *     <li>遍历每个Key，使用ZCOUNT统计窗口内的请求数</li>
     *     <li>累加所有Key的计数并返回</li>
     * </ol>
     * 
     * <p>注意：KEYS命令在大数据量场景下可能阻塞Redis，
     * 生产环境建议使用SCAN命令替代。</p>
     * 
     * @param ipAddress 用户IP地址
     * @return 当前周期内的请求数，如果Redis未启用或执行失败返回0
     */
    public int getPeriodCount(String ipAddress) {
        if (!behaviorRedisConfig.isRedisEnabled()) {
            return 0;
        }

        try {
            long currentTime = System.currentTimeMillis();
            long windowSize = securityConfig.getPeriodMinutes() * 60 * 1000L;
            long windowStart = currentTime - windowSize;

            String keyPattern = PERIOD_COUNT_KEY_PREFIX + ipAddress + ":*";
            Set<String> keys = redisTemplate.keys(keyPattern);

            if (keys == null || keys.isEmpty()) {
                return 0;
            }

            int totalCount = 0;
            for (String key : keys) {
                Long count = redisTemplate.opsForZSet().count(key, windowStart, currentTime);
                if (count != null) {
                    totalCount += count.intValue();
                }
            }

            return totalCount;
        } catch (Exception e) {
            logger.error("Failed to get period count for IP: {}", ipAddress, e);
            return 0;
        }
    }

    /**
     * 获取当前日期的请求数
     * 查询Redis中存储的每日计数值
     * 
     * <p>实现原理：</p>
     * <ol>
     *     <li>获取当前日期并构造Redis Key</li>
     *     <li>使用GET命令获取计数值</li>
     *     <li>将Long类型转换为int类型返回</li>
     * </ol>
     * 
     * @param ipAddress 用户IP地址
     * @return 当前日期的请求数，如果Key不存在或执行失败返回0
     */
    public int getDailyCount(String ipAddress) {
        if (!behaviorRedisConfig.isRedisEnabled()) {
            return 0;
        }

        try {
            String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String key = DAILY_COUNT_KEY_PREFIX + ipAddress + ":" + date;

            Object countObj = redisTemplate.opsForValue().get(key);
            if (countObj instanceof Long) {
                return ((Long) countObj).intValue();
            }
            return 0;
        } catch (Exception e) {
            logger.error("Failed to get daily count for IP: {}", ipAddress, e);
            return 0;
        }
    }

    /**
     * 清理过期的周期计数数据
     * 移除滑动窗口之前的过期时间戳记录
     * 
     * <p>实现原理：</p>
     * <ol>
     *     <li>计算滑动窗口的起始时间点</li>
     *     <li>使用KEYS命令匹配该IP的所有周期计数Key</li>
     *     <li>遍历每个Key，使用ZREMRANGEBYSCORE移除过期数据</li>
     *     <li>保留窗口内的有效数据用于后续查询</li>
     * </ol>
     * 
     * <p>该方法用于手动清理数据，减轻Redis内存压力。
     * 通常情况下依赖Key的过期时间自动清理即可。</p>
     * 
     * @param ipAddress 用户IP地址
     */
    public void cleanupPeriodData(String ipAddress) {
        if (!behaviorRedisConfig.isRedisEnabled()) {
            return;
        }

        try {
            long currentTime = System.currentTimeMillis();
            long windowSize = securityConfig.getPeriodMinutes() * 60 * 1000L;
            long windowStart = currentTime - windowSize;

            String keyPattern = PERIOD_COUNT_KEY_PREFIX + ipAddress + ":*";
            Set<String> keys = redisTemplate.keys(keyPattern);

            if (keys != null) {
                for (String key : keys) {
                    redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart - 1);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to cleanup period data for IP: {}", ipAddress, e);
        }
    }

    /**
     * 重置指定IP的所有计数
     * 删除该IP的周期计数和每日计数的所有Redis Key
     * 
     * <p>实现原理：</p>
     * <ol>
     *     <li>使用KEYS命令匹配该IP的所有周期计数Key</li>
     *     <li>使用KEYS命令匹配该IP的所有每日计数Key</li>
     *     <li>使用DEL命令批量删除匹配的Key</li>
     *     <li>记录操作日志</li>
     * </ol>
     * 
     * <p>该方法用于手动重置某个IP的访问计数，
     * 通常在解封被限制的IP时使用。</p>
     * 
     * @param ipAddress 用户IP地址
     */
    public void resetAllCounts(String ipAddress) {
        if (!behaviorRedisConfig.isRedisEnabled()) {
            return;
        }

        try {
            String periodPattern = PERIOD_COUNT_KEY_PREFIX + ipAddress + ":*";
            String dailyPattern = DAILY_COUNT_KEY_PREFIX + ipAddress + ":*";

            Set<String> periodKeys = redisTemplate.keys(periodPattern);
            Set<String> dailyKeys = redisTemplate.keys(dailyPattern);

            if (periodKeys != null && !periodKeys.isEmpty()) {
                redisTemplate.delete(periodKeys);
            }
            if (dailyKeys != null && !dailyKeys.isEmpty()) {
                redisTemplate.delete(dailyKeys);
            }

            logger.info("Reset all counts for IP: {}", ipAddress);
        } catch (Exception e) {
            logger.error("Failed to reset counts for IP: {}", ipAddress, e);
        }
    }
}
