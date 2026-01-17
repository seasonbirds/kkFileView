package cn.keking.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 独立的Redis配置类，用于用户行为监控的访问计数
 * 不影响系统中现有的Redis配置，实现独立的高性能计数
 */
@Configuration
public class BehaviorRedisConfig {

    /**
     * 是否启用Redis计数
     */
    @Value("${security.behavior.redis.enabled:false}")
    private boolean redisEnabled;

    /**
     * Redis服务器地址
     */
    @Value("${security.behavior.redis.host:localhost}")
    private String redisHost;

    /**
     * Redis服务器端口
     */
    @Value("${security.behavior.redis.port:6379}")
    private int redisPort;

    /**
     * Redis服务器密码
     */
    @Value("${security.behavior.redis.password:}")
    private String redisPassword;

    /**
     * Redis数据库编号
     */
    @Value("${security.behavior.redis.database:0}")
    private int redisDatabase;

    /**
     * 创建独立的Redis连接工厂
     */
    @Bean("behaviorRedisConnectionFactory")
    public RedisConnectionFactory behaviorRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(config);
    }

    /**
     * 创建独立的RedisTemplate，用于用户行为监控
     */
    @Bean("behaviorRedisTemplate")
    public RedisTemplate<String, Object> behaviorRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(behaviorRedisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 判断是否启用Redis计数
     * @return 是否启用Redis
     */
    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    /**
     * 获取Redis服务器地址
     * @return Redis服务器地址
     */
    public String getRedisHost() {
        return redisHost;
    }

    /**
     * 获取Redis服务器端口
     * @return Redis服务器端口
     */
    public int getRedisPort() {
        return redisPort;
    }

    /**
     * 获取Redis服务器密码
     * @return Redis服务器密码
     */
    public String getRedisPassword() {
        return redisPassword;
    }

    /**
     * 获取Redis数据库编号
     * @return Redis数据库编号
     */
    public int getRedisDatabase() {
        return redisDatabase;
    }
}
