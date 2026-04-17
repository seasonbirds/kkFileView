package cn.keking.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnProperty(name = "user.redis.enabled", havingValue = "true")
public class UserRedisConfig {

    @Value("${user.redis.host:127.0.0.1}")
    private String host;

    @Value("${user.redis.port:6379}")
    private int port;

    @Value("${user.redis.password:}")
    private String password;

    @Value("${user.redis.database:1}")
    private int database;

    @Bean(name = "userRedisConnectionFactory")
    public RedisConnectionFactory userRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setDatabase(database);
        if (StringUtils.isNotBlank(password)) {
            config.setPassword(password);
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean(name = "userRedisTemplate")
    public RedisTemplate<String, Object> userRedisTemplate(RedisConnectionFactory userRedisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(userRedisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return template;
    }

    @Bean(name = "userStringRedisTemplate")
    public StringRedisTemplate userStringRedisTemplate(RedisConnectionFactory userRedisConnectionFactory) {
        return new StringRedisTemplate(userRedisConnectionFactory);
    }
}
