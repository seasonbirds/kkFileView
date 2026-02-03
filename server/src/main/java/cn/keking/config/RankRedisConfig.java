package cn.keking.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件预览排行榜Redis配置类
 * 独立的Redis配置，不依赖原有的cache.type配置
 *
 * @author kl
 * @since 2024/01/01
 */
@Configuration
@ConditionalOnProperty(name = "cache.preview.rank.enabled", havingValue = "true", matchIfMissing = true)
public class RankRedisConfig {

    @Value("${cache.preview.rank.address:127.0.0.1:6379}")
    private String address;

    @Value("${cache.preview.rank.password:}")
    private String password;

    @Value("${cache.preview.rank.database:0}")
    private int database;

    @Value("${cache.preview.rank.connection.minimum.idle.size:10}")
    private int connectionMinimumIdleSize;

    @Value("${cache.preview.rank.connection.pool.size:64}")
    private int connectionPoolSize;

    @Bean(name = "rankRedissonClient", destroyMethod = "shutdown")
    public RedissonClient rankRedissonClient() {
        Config config = new Config();
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress("redis://" + address)
                .setDatabase(database)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setConnectionPoolSize(connectionPoolSize);

        if (password != null && !password.isEmpty()) {
            serverConfig.setPassword(password);
        }

        return Redisson.create(config);
    }
}
