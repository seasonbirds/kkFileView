package cn.keking.config;

import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

/**
 * 业务系统Redis配置类
 * 用于连接业务系统的Redis，获取用户登录信息
 * 与系统自身的缓存Redis配置相互独立，互不影响
 *
 * <p>启用条件：
 * <ul>
 *     <li>配置 auth.redis.address 后自动启用</li>
 *     <li>不配置则不创建此Bean，认证功能不启用</li>
 * </ul>
 *
 * @author kkFileView
 * @since 2026/04/15
 */
@ConfigurationProperties(prefix = "auth.redis")
@Configuration
@ConditionalOnProperty(prefix = "auth.redis", name = "address", havingValue = ".+", matchIfMissing = false)
public class AuthRedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuthRedisConfig.class);

    /**
     * Redis服务器地址，格式：host:port
     * 例如：127.0.0.1:6379
     */
    private String address;

    /**
     * Redis密码，无密码则为null
     */
    private String password;

    /**
     * Redis数据库索引，默认为0
     */
    private int database = 0;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 10000;

    /**
     * 命令等待超时时间（毫秒）
     */
    private int timeout = 3000;

    /**
     * 连接池大小
     */
    private int connectionPoolSize = 64;

    /**
     * 连接池最小空闲连接数
     */
    private int connectionMinimumIdleSize = 10;

    /**
     * 重试次数
     */
    private int retryAttempts = 3;

    /**
     * 重试间隔（毫秒）
     */
    private int retryInterval = 1500;

    /**
     * 编码解码器
     */
    private String codec = "org.redisson.codec.JsonJacksonCodec";

    /**
     * 创建业务系统Redis客户端
     * 该客户端独立于系统缓存Redis，专门用于获取用户登录信息
     *
     * @return RedissonClient Redis客户端实例
     * @throws Exception 创建客户端时可能抛出的异常
     */
    @Bean(name = "authRedissonClient")
    public RedissonClient authRedissonClient() throws Exception {
        logger.info("初始化业务系统Redis客户端，地址: {}", address);

        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + address)
                .setDatabase(database)
                .setConnectionPoolSize(connectionPoolSize)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setConnectTimeout(connectTimeout)
                .setTimeout(timeout)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setPassword(StringUtils.trimToNull(password));

        Codec codecInstance = (Codec) ClassUtils.forName(getCodec(), ClassUtils.getDefaultClassLoader()).newInstance();
        config.setCodec(codecInstance);
        config.setEventLoopGroup(new NioEventLoopGroup());

        return Redisson.create(config);
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

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public void setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    public int getConnectionMinimumIdleSize() {
        return connectionMinimumIdleSize;
    }

    public void setConnectionMinimumIdleSize(int connectionMinimumIdleSize) {
        this.connectionMinimumIdleSize = connectionMinimumIdleSize;
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
}
