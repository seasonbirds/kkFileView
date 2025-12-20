package cn.keking.config;

import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

/**
 * SSO单点登录功能专用Redis配置类
 * 独立于系统原有Redis配置，避免影响业务功能
 */
@ConfigurationProperties(prefix = "spring.redisson")
@Configuration
public class SSORedisConfig {
    private String address;
    private int database;
    private String password;
    private int connectionPoolSize;
    private int connectionMinimumIdleSize;
    private int idleConnectionTimeout;
    private int pingTimeout;
    private int connectTimeout;
    private int timeout;
    private int retryAttempts;
    private int retryInterval;
    private int reconnectionTimeout;
    private int failedAttempts;
    private String subscriptionsPerConnection;
    private String clientName;
    private String codec;
    private int threads;
    private int nettyThreads;
    private String transportMode;

    @Bean(name = "ssoRedissonClient")
    public RedissonClient redissonClient() throws Exception {
        Config config = new Config();
        if (codec != null) {
            config.setCodec((Codec) ClassUtils.forName(codec, ClassUtils.getDefaultClassLoader()).newInstance());
        }
        if (threads != 0) {
            config.setThreads(threads);
        }
        if (nettyThreads != 0) {
            config.setNettyThreads(nettyThreads);
        }
        if (transportMode != null) {
            config.setTransportMode(TransportMode.valueOf(transportMode.toUpperCase()));
        }
        SingleServerConfig singleServerConfig = config.useSingleServer();
        singleServerConfig.setAddress(address);
        singleServerConfig.setDatabase(database);
        if (password != null && !"null".equals(password) && !"".equals(password)) {
            singleServerConfig.setPassword(password);
        }
        singleServerConfig.setConnectionPoolSize(connectionPoolSize);
        singleServerConfig.setConnectionMinimumIdleSize(connectionMinimumIdleSize);
        singleServerConfig.setIdleConnectionTimeout(idleConnectionTimeout);
        singleServerConfig.setPingTimeout(pingTimeout);
        singleServerConfig.setConnectTimeout(connectTimeout);
        singleServerConfig.setTimeout(timeout);
        singleServerConfig.setRetryAttempts(retryAttempts);
        singleServerConfig.setRetryInterval(retryInterval);
        singleServerConfig.setReconnectionTimeout(reconnectionTimeout);
        singleServerConfig.setFailedAttempts(failedAttempts);
        if (StringUtils.isNotBlank(subscriptionsPerConnection)) {
            singleServerConfig.setSubscriptionsPerConnection(Integer.parseInt(subscriptionsPerConnection));
        }
        if (StringUtils.isNotBlank(clientName)) {
            singleServerConfig.setClientName(clientName);
        }
        return Redisson.create(config);
    }

    // Getter和Setter方法
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public int getIdleConnectionTimeout() {
        return idleConnectionTimeout;
    }

    public void setIdleConnectionTimeout(int idleConnectionTimeout) {
        this.idleConnectionTimeout = idleConnectionTimeout;
    }

    public int getPingTimeout() {
        return pingTimeout;
    }

    public void setPingTimeout(int pingTimeout) {
        this.pingTimeout = pingTimeout;
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

    public int getReconnectionTimeout() {
        return reconnectionTimeout;
    }

    public void setReconnectionTimeout(int reconnectionTimeout) {
        this.reconnectionTimeout = reconnectionTimeout;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public String getSubscriptionsPerConnection() {
        return subscriptionsPerConnection;
    }

    public void setSubscriptionsPerConnection(String subscriptionsPerConnection) {
        this.subscriptionsPerConnection = subscriptionsPerConnection;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getNettyThreads() {
        return nettyThreads;
    }

    public void setNettyThreads(int nettyThreads) {
        this.nettyThreads = nettyThreads;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }
}