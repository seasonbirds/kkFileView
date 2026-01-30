package cn.keking.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件预览统计功能专用Redis配置
 * @author kkfileview
 */
@Configuration
public class PreviewCountRedisConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(PreviewCountRedisConfig.class);
    
    @Value("${preview-count.redis.address:127.0.0.1:6379}")
    private String redisAddress;
    
    @Value("${preview-count.redis.password:}")
    private String redisPassword;
    
    @Value("${preview-count.redis.database:0}")
    private int redisDatabase;
    
    @Value("${preview-count.redis.timeout:3000}")
    private int redisTimeout;
    
    @Value("${preview-count.redis.pool.maxActive:8}")
    private int maxActive;
    
    @Value("${preview-count.redis.pool.maxIdle:8}")
    private int maxIdle;
    
    @Value("${preview-count.redis.pool.minIdle:0}")
    private int minIdle;
    
    @Bean(name = "previewCountRedissonClient")
    public RedissonClient previewCountRedissonClient() {
        try {
            Config config = new Config();
            
            // 设置Redis服务器地址
            String address = redisAddress.startsWith("redis://") ? redisAddress : "redis://" + redisAddress;
            config.useSingleServer()
                  .setAddress(address)
                  .setDatabase(redisDatabase)
                  .setTimeout(redisTimeout)
                  .setConnectionPoolSize(maxActive)
                  .setConnectionMinimumIdleSize(minIdle)
                  .setConnectionMaximumIdleSize(maxIdle);
            
            // 设置密码（如果有的话）
            if (redisPassword != null && !redisPassword.isEmpty()) {
                config.useSingleServer().setPassword(redisPassword);
            }
            
            logger.info("初始化文件预览统计Redis客户端: {}", address);
            return Redisson.create(config);
        } catch (Exception e) {
            logger.error("初始化文件预览统计Redis客户端失败", e);
            throw new RuntimeException("初始化文件预览统计Redis客户端失败", e);
        }
    }
}