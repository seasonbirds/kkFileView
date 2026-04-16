package cn.keking.config;

import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

/**
 * 下载限流Redis配置类
 * <p>
 * 提供独立的Redis客户端配置，用于下载源文件接口的限流功能。
 * 与系统原有缓存Redis配置完全隔离，互不影响。
 * <p>
 * 配置前缀：download.redis
 *
 * @author keking
 */
@Configuration
@ConfigurationProperties(prefix = "download.redis")
public class DownloadRedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(DownloadRedisConfig.class);

    private String address = "127.0.0.1:6379";
    private String password = null;
    private int database = 1;
    private int connectionMinimumIdleSize = 5;
    private int connectionPoolSize = 32;
    private int idleConnectionTimeout = 10000;
    private int connectTimeout = 10000;
    private int timeout = 3000;
    private int retryAttempts = 3;
    private int retryInterval = 1500;
    private String codec = "org.redisson.codec.JsonJacksonCodec";
    private int thread;

    private RedissonClient redissonClient;

    /**
     * 创建下载限流专用的Redisson客户端
     * <p>
     * 该客户端与系统原有缓存Redis客户端完全隔离，
     * 使用独立的配置和连接池。
     *
     * @return RedissonClient实例
     */
    @Bean(name = "downloadRedissonClient")
    public RedissonClient downloadRedissonClient() {
        if (redissonClient != null) {
            return redissonClient;
        }
        try {
            Config config = createConfig();
            redissonClient = org.redisson.Redisson.create(config);
            logger.info("Download Redis client initialized successfully, address: {}", address);
            return redissonClient;
        } catch (Exception e) {
            logger.error("Failed to initialize download Redis client", e);
            throw new RuntimeException("Failed to initialize download Redis client", e);
        }
    }

    /**
     * 创建Redisson配置
     *
     * @return Redisson配置对象
     * @throws Exception 配置创建异常
     */
    private Config createConfig() throws Exception {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + address)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setConnectionPoolSize(connectionPoolSize)
                .setDatabase(database)
                .setIdleConnectionTimeout(idleConnectionTimeout)
                .setConnectTimeout(connectTimeout)
                .setTimeout(timeout)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setPassword(StringUtils.trimToNull(password));

        Codec codecInstance = (Codec) ClassUtils.forName(getCodec(), ClassUtils.getDefaultClassLoader()).newInstance();
        config.setCodec(codecInstance);
        if (thread > 0) {
            config.setThreads(thread);
        }
        config.setEventLoopGroup(new NioEventLoopGroup());
        return config;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public int getConnectionMinimumIdleSize() {
        return connectionMinimumIdleSize;
    }

    public void setConnectionMinimumIdleSize(int connectionMinimumIdleSize) {
        this.connectionMinimumIdleSize = connectionMinimumIdleSize;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public void setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    public int getIdleConnectionTimeout() {
        return idleConnectionTimeout;
    }

    public void setIdleConnectionTimeout(int idleConnectionTimeout) {
        this.idleConnectionTimeout = idleConnectionTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public int getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public int getThread() {
        return thread;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }
}
