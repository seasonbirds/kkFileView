package cn.keking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 单点登录配置
 * @author AI Assistant
 */
@Component
@ConfigurationProperties(prefix = "sso")
public class SsoConfig {
    /**
     * 是否启用单点登录
     */
    private boolean enabled = false;
    
    /**
     * 登录校验的Token名称
     */
    private String tokenName = "TOKEN";
    
    /**
     * 登录失败后跳转的URL
     */
    private String loginUrl = "/login";
    
    /**
     * Redis配置
     */
    private RedisConfig redis = new RedisConfig();
    
    public static class RedisConfig {
        /**
         * Redis服务器地址
         */
        private String host = "localhost";
        
        /**
         * Redis服务器端口
         */
        private int port = 6379;
        
        /**
         * Redis密码
         */
        private String password = "";
        
        /**
         * Redis数据库索引
         */
        private int database = 0;
        
        /**
         * Redis连接超时时间（毫秒）
         */
        private int timeout = 3000;
        
        /**
         * 用户信息存储的前缀
         */
        private String userPrefix = "USER-";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
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

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public String getUserPrefix() {
            return userPrefix;
        }

        public void setUserPrefix(String userPrefix) {
            this.userPrefix = userPrefix;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public RedisConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisConfig redis) {
        this.redis = redis;
    }
}