package cn.keking.config;

import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

/**
 * 用户模块独立Redis配置类
 * 用于存储用户Token等用户相关数据，与系统现有Redis配置完全独立
 * 不影响系统原有的Redisson配置和功能
 */
@Configuration
public class UserRedisConfig {

    /**
     * Redis主机地址
     */
    @Value("${user.redis.host:127.0.0.1}")
    private String host;

    /**
     * Redis端口
     */
    @Value("${user.redis.port:6379}")
    private int port;

    /**
     * Redis密码
     */
    @Value("${user.redis.password:}")
    private String password;

    /**
     * Redis数据库编号（使用独立的数据库，避免与系统现有Redis冲突）
     */
    @Value("${user.redis.database:1}")
    private int database;

    /**
     * 连接池最小空闲连接数
     */
    @Value("${user.redis.connectionMinimumIdleSize:10}")
    private int connectionMinimumIdleSize;

    /**
     * 连接超时时间（毫秒）
     */
    @Value("${user.redis.connectTimeout:10000}")
    private int connectTimeout;

    /**
     * 命令等待超时时间（毫秒）
     */
    @Value("${user.redis.timeout:3000}")
    private int timeout;

    /**
     * 连接池大小
     */
    @Value("${user.redis.connectionPoolSize:64}")
    private int connectionPoolSize;

    /**
     * 编码解码器
     */
    @Value("${user.redis.codec:org.redisson.codec.JsonJacksonCodec}")
    private String codec;

    /**
     * 线程数（当前处理核数量 * 2）
     */
    @Value("${user.redis.thread:4}")
    private int thread;

    /**
     * 创建用户模块独立的Redisson配置
     * @return Redisson配置对象
     * @throws Exception 配置创建异常
     */
    @Bean(name = "userRedissonConfig")
    public Config userRedissonConfig() throws Exception {
        Config config = new Config();
        
        // 构建Redis地址
        String address = "redis://" + host + ":" + port;
        
        // 配置单节点模式
        config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setConnectionPoolSize(connectionPoolSize)
                .setDatabase(database)
                .setConnectTimeout(connectTimeout)
                .setTimeout(timeout)
                .setPassword(StringUtils.trimToNull(password));
        
        // 设置编码解码器
        Codec codecInstance = (Codec) ClassUtils.forName(codec, ClassUtils.getDefaultClassLoader()).newInstance();
        config.setCodec(codecInstance);
        
        // 设置线程数和事件循环组
        config.setThreads(thread);
        config.setEventLoopGroup(new NioEventLoopGroup());
        
        return config;
    }

    /**
     * 创建用户模块独立的Redisson客户端
     * @param userRedissonConfig 用户模块Redis配置
     * @return Redisson客户端
     */
    @Bean(name = "userRedissonClient")
    public RedissonClient userRedissonClient(Config userRedissonConfig) {
        return Redisson.create(userRedissonConfig);
    }
}
