package cn.keking.config;

import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.lang3.StringUtils;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

/**
 * 文件预览统计专用的Redisson客户端配置
 * 该配置独立于主缓存配置，确保统计功能始终可用
 * 
 * @author kkfileview
 */
@Configuration
@ConditionalOnProperty(name = "file.preview.statistics.enabled", havingValue = "true", matchIfMissing = true)
public class FilePreviewStatisticsRedissonConfig {

    @Value("${spring.redisson.address:127.0.0.1:6379}")
    private String address;

    @Value("${spring.redisson.password:}")
    private String password;

    @Value("${spring.redisson.connectionPoolSize:64}")
    private int connectionPoolSize;

    @Value("${spring.redisson.connectionMinimumIdleSize:10}")
    private int connectionMinimumIdleSize;

    @Value("${spring.redisson.connectTimeout:10000}")
    private int connectTimeout;

    @Value("${spring.redisson.timeout:3000}")
    private int timeout;

    @Value("${spring.redisson.retryAttempts:3}")
    private int retryAttempts;

    @Value("${spring.redisson.retryInterval:1500}")
    private int retryInterval;

    @Value("${spring.redisson.database:0}")
    private int database;

    @Value("${spring.redisson.codec:org.redisson.codec.JsonJacksonCodec}")
    private String codec;

    @Value("${spring.redisson.thread:}")
    private Integer thread;

    /**
     * 创建专用于文件预览统计的RedissonClient
     * 
     * @return Redisson客户端实例
     * @throws Exception 配置异常
     */
    @Bean(name = "statisticsRedissonClient")
    public org.redisson.api.RedissonClient statisticsRedissonClient() throws Exception {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setConnectionPoolSize(connectionPoolSize)
                .setDatabase(database)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setTimeout(timeout)
                .setConnectTimeout(connectTimeout)
                .setPassword(StringUtils.trimToNull(password));
        
        Codec codecInstance = (Codec) ClassUtils.forName(codec, ClassUtils.getDefaultClassLoader()).newInstance();
        config.setCodec(codecInstance);
        
        if (thread != null) {
            config.setThreads(thread);
            config.setEventLoopGroup(new NioEventLoopGroup());
        }
        
        return org.redisson.Redisson.create(config);
    }
}