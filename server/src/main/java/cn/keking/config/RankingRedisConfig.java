package cn.keking.config;

import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.lang3.StringUtils;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import jakarta.annotation.PostConstruct;

/**
 * 排行榜Redis配置
 * 独立于cache.type配置，用于文件预览排行榜功能
 *
 * @author kl
 * @date 2026/02/25
 */
@Configuration
@ConditionalOnProperty(prefix = "ranking.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RankingRedisConfig {

    @Value("${ranking.redis.address:redis://127.0.0.1:6379}")
    private String address;

    @Value("${ranking.redis.connection-minimum-idle-size:10}")
    private int connectionMinimumIdleSize = 10;

    @Value("${ranking.redis.idle-connection-timeout:10000}")
    private int idleConnectionTimeout = 10000;

    @Value("${ranking.redis.ping-timeout:1000}")
    private int pingTimeout = 1000;

    @Value("${ranking.redis.connect-timeout:10000}")
    private int connectTimeout = 10000;

    @Value("${ranking.redis.timeout:3000}")
    private int timeout = 3000;

    @Value("${ranking.redis.retry-attempts:3}")
    private int retryAttempts = 3;

    @Value("${ranking.redis.retry-interval:1500}")
    private int retryInterval = 1500;

    @Value("${ranking.redis.password:}")
    private String password;

    @Value("${ranking.redis.client-name:kkfileview-ranking}")
    private String clientName;

    @Value("${ranking.redis.subscription-connection-minimum-idle-size:1}")
    private int subscriptionConnectionMinimumIdleSize = 1;

    @Value("${ranking.redis.subscription-connection-pool-size:50}")
    private int subscriptionConnectionPoolSize = 50;

    @Value("${ranking.redis.connection-pool-size:64}")
    private int connectionPoolSize = 64;

    @Value("${ranking.redis.database:0}")
    private int database = 0;

    @Value("${ranking.redis.dns-monitoring-interval:5000}")
    private int dnsMonitoringInterval = 5000;

    @Value("${ranking.redis.codec:org.redisson.codec.JsonJacksonCodec}")
    private String codec = "org.redisson.codec.JsonJacksonCodec";

    @Value("${ranking.redis.threads:0}")
    private int threads;

    private Config config;

    @PostConstruct
    public void init() throws Exception {
        config = new Config();
        config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setConnectionPoolSize(connectionPoolSize)
                .setDatabase(database)
                .setDnsMonitoringInterval(dnsMonitoringInterval)
                .setSubscriptionConnectionMinimumIdleSize(subscriptionConnectionMinimumIdleSize)
                .setSubscriptionConnectionPoolSize(subscriptionConnectionPoolSize)
                .setClientName(clientName)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setTimeout(timeout)
                .setConnectTimeout(connectTimeout)
                .setIdleConnectionTimeout(idleConnectionTimeout)
                .setPassword(StringUtils.trimToNull(password));

        Codec codecInstance = (Codec) ClassUtils.forName(codec, ClassUtils.getDefaultClassLoader()).getDeclaredConstructor().newInstance();
        config.setCodec(codecInstance);

        if (threads > 0) {
            config.setThreads(threads);
        }
        config.setEventLoopGroup(new NioEventLoopGroup());
    }

    @Bean(name = "rankingRedissonConfig")
    public Config rankingRedissonConfig() {
        return config;
    }
}
