package cn.keking.config;

import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

/**
 * 独立的Redis配置，用于文件预览排名功能
 * 不依赖于原有cache.type配置，确保排名功能始终可用
 * 
 * @author Trae AI
 * @since 2024/03/29
 */
@ConfigurationProperties(prefix = "ranking.redis")
@Configuration
public class RankingRedisConfig {

    private String address = "redis://127.0.0.1:6379";
    private int connectionMinimumIdleSize = 5;
    private int idleConnectionTimeout = 10000;
    private int pingTimeout = 1000;
    private int connectTimeout = 10000;
    private int timeout = 3000;
    private int retryAttempts = 3;
    private int retryInterval = 1500;
    private String password = null;
    private int subscriptionsPerConnection = 5;
    private String clientName = "kkFileView-Ranking";
    private int connectionPoolSize = 32;
    private int database = 1;
    private int dnsMonitoringInterval = 5000;
    private int thread = 4;
    private String codec = "org.redisson.codec.JsonJacksonCodec";

    @Bean(name = "rankingRedissonClient")
    public RedissonClient rankingRedissonClient() {
        try {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress(address)
                    .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                    .setConnectionPoolSize(connectionPoolSize)
                    .setDatabase(database)
                    .setDnsMonitoringInterval(dnsMonitoringInterval)
                    .setSubscriptionsPerConnection(subscriptionsPerConnection)
                    .setClientName(clientName)
                    .setRetryAttempts(retryAttempts)
                    .setRetryInterval(retryInterval)
                    .setTimeout(timeout)
                    .setConnectTimeout(connectTimeout)
                    .setIdleConnectionTimeout(idleConnectionTimeout)
                    .setPassword(StringUtils.trimToNull(password));
            
            Codec codecInstance = (Codec) ClassUtils.forName(getCodec(), 
                    ClassUtils.getDefaultClassLoader()).newInstance();
            config.setCodec(codecInstance);
            config.setThreads(thread);
            config.setEventLoopGroup(new NioEventLoopGroup());
            
            return Redisson.create(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ranking Redis client", e);
        }
    }

    // Getters and Setters
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getSubscriptionsPerConnection() {
        return subscriptionsPerConnection;
    }

    public void setSubscriptionsPerConnection(int subscriptionsPerConnection) {
        this.subscriptionsPerConnection = subscriptionsPerConnection;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public int getConnectionPoolSize() {
        return connectionPoolSize;
    }

    public void setConnectionPoolSize(int connectionPoolSize) {
        this.connectionPoolSize = connectionPoolSize;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public int getDnsMonitoringInterval() {
        return dnsMonitoringInterval;
    }

    public void setDnsMonitoringInterval(int dnsMonitoringInterval) {
        this.dnsMonitoringInterval = dnsMonitoringInterval;
    }

    public int getThread() {
        return thread;
    }

    public void setThread(int thread) {
        this.thread = thread;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }
}
