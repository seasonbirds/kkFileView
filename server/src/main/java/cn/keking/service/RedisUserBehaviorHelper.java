package cn.keking.service;

import cn.keking.config.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Protocol;

/**
 * Redis用户行为统计助手
 * 使用独立的Redis配置，避免影响现有Redis功能
 *
 * @author keking
 * @since 2025-07-17
 */
public class RedisUserBehaviorHelper {

    private static final Logger logger = LoggerFactory.getLogger(RedisUserBehaviorHelper.class);
    private static RedisUserBehaviorHelper instance;
    private JedisPool jedisPool;

    // Redis key前缀
    private static final String PERIOD_KEY_PREFIX = "user:behavior:period:";
    private static final String DAILY_KEY_PREFIX = "user:behavior:daily:";

    private RedisUserBehaviorHelper() {
        initJedisPool();
    }

    public static synchronized RedisUserBehaviorHelper getInstance() {
        if (instance == null) {
            instance = new RedisUserBehaviorHelper();
        }
        return instance;
    }

    /**
     * 初始化Jedis连接池
     */
    private void initJedisPool() {
        try {
            String host = ConfigConstants.getUserBehaviorAnalysisRedisHost();
            int port = ConfigConstants.getUserBehaviorAnalysisRedisPort();
            String password = ConfigConstants.getUserBehaviorAnalysisRedisPassword();
            int database = ConfigConstants.getUserBehaviorAnalysisRedisDatabase();

            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(10);
            config.setMaxIdle(5);
            config.setMinIdle(1);
            config.setTestOnBorrow(true);
            config.setTestOnReturn(true);

            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(config, host, port, Protocol.DEFAULT_TIMEOUT, password, database);
            } else {
                jedisPool = new JedisPool(config, host, port, Protocol.DEFAULT_TIMEOUT, null, database);
            }

            logger.info("Redis用户行为助手初始化成功，host: {}, port: {}, database: {}", host, port, database);
        } catch (Exception e) {
            logger.error("Redis用户行为助手初始化失败", e);
            throw new RuntimeException("Redis用户行为助手初始化失败", e);
        }
    }

    /**
     * 增加用户请求计数
     *
     * @param ipAddress 用户IP地址
     * @param periodMinutes 统计周期（分钟）
     */
    public void incrementRequestCount(String ipAddress, int periodMinutes) {
        try (Jedis jedis = jedisPool.getResource()) {
            // 周期计数（带过期时间）
            String periodKey = PERIOD_KEY_PREFIX + ipAddress;
            jedis.incr(periodKey);
            jedis.expire(periodKey, periodMinutes * 60);

            // 每日计数（按日期分组，过期时间为1天）
            String dateKey = DAILY_KEY_PREFIX + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            jedis.hincrBy(dateKey, ipAddress, 1);
            jedis.expire(dateKey, 24 * 60 * 60);
        } catch (Exception e) {
            logger.error("增加请求计数失败，IP: {}", ipAddress, e);
            // 忽略Redis错误，不影响正常请求
        }
    }

    /**
     * 获取用户在指定周期内的请求次数
     *
     * @param ipAddress 用户IP地址
     * @return 请求次数
     */
    public int getPeriodRequestCount(String ipAddress) {
        try (Jedis jedis = jedisPool.getResource()) {
            String periodKey = PERIOD_KEY_PREFIX + ipAddress;
            String count = jedis.get(periodKey);
            return count != null ? Integer.parseInt(count) : 0;
        } catch (Exception e) {
            logger.error("获取周期请求计数失败，IP: {}", ipAddress, e);
            return 0;
        }
    }

    /**
     * 获取用户当天的请求次数
     *
     * @param ipAddress 用户IP地址
     * @return 请求次数
     */
    public int getDailyRequestCount(String ipAddress) {
        try (Jedis jedis = jedisPool.getResource()) {
            String dateKey = DAILY_KEY_PREFIX + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String count = jedis.hget(dateKey, ipAddress);
            return count != null ? Integer.parseInt(count) : 0;
        } catch (Exception e) {
            logger.error("获取每日请求计数失败，IP: {}", ipAddress, e);
            return 0;
        }
    }

    /**
     * 关闭连接池
     */
    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}
