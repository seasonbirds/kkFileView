package cn.keking.service;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

/**
 * 用户行为监控Redisson客户端服务
 * @author: system
 * @since: 2024/01/01
 */
@Configuration
@ConditionalOnExpression("'${user.behavior.cache.type:jdk}'.equals('redis')")
public class UserBehaviorRedissonClientService {

    @Bean(name = "userBehaviorRedissonClient")
    public RedissonClient userBehaviorRedissonClient(@Qualifier("userBehaviorRedissonConfig") Config config) {
        return Redisson.create(config);
    }
}