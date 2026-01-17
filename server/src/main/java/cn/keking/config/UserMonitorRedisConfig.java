package cn.keking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class UserMonitorRedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(UserMonitorRedisConfig.class);

    @Value("${user.monitor.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${user.monitor.redis.port:6379}")
    private int redisPort;

    @Value("${user.monitor.redis.password:}")
    private String redisPassword;

    @Value("${user.monitor.redis.database:1}")
    private int redisDatabase;

    @Value("${user.monitor.redis.timeout:2000}")
    private int redisTimeout;

    @Value("${user.monitor.redis.max.total:8}")
    private int maxTotal;

    @Value("${user.monitor.redis.max.idle:8}")
    private int maxIdle;

    @Value("${user.monitor.redis.min.idle:0}")
    private int minIdle;

    @Value("${user.monitor.redis.enabled:false}")
    private boolean redisEnabled;

    @Bean(name = "userMonitorJedisPool")
    public JedisPool userMonitorJedisPool() {
        if (!redisEnabled) {
            logger.info("用户监控Redis未启用，将使用内存缓存");
            return null;
        }

        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(maxTotal);
            poolConfig.setMaxIdle(maxIdle);
            poolConfig.setMinIdle(minIdle);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);

            JedisPool pool;
            if (redisPassword == null || redisPassword.isEmpty()) {
                pool = new JedisPool(poolConfig, redisHost, redisPort, redisTimeout, null, redisDatabase);
            } else {
                pool = new JedisPool(poolConfig, redisHost, redisPort, redisTimeout, redisPassword, redisDatabase);
            }

            logger.info("用户监控Redis连接池初始化成功: {}:{}", redisHost, redisPort);
            return pool;
        } catch (Exception e) {
            logger.error("用户监控Redis连接池初始化失败，将使用内存缓存", e);
            return null;
        }
    }

    @Bean(name = "userMonitorRedisEnabled")
    public boolean userMonitorRedisEnabled() {
        return redisEnabled && userMonitorJedisPool() != null;
    }
}
