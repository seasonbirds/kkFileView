package cn.keking.service;

import cn.keking.config.UserConfigConstants;
import cn.keking.model.User;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务类
 * 提供用户登录、登出、Token验证等功能
 * 使用独立的Redis存储用户Token，支持分布式部署
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * 用户Token存储前缀（手机号 -> Token）
     */
    private static final String USER_TOKEN_PREFIX = "user:token:";

    /**
     * Token用户映射前缀（Token -> 手机号）
     */
    private static final String TOKEN_USER_PREFIX = "token:user:";

    /**
     * 用户模块独立的Redisson客户端
     */
    @Resource(name = "userRedissonClient")
    private RedissonClient userRedissonClient;

    /**
     * 用户登录
     * 验证用户名和密码，成功后生成Token并存储到Redis
     * 
     * @param phone 手机号
     * @param password 密码
     * @return 登录成功返回Token，失败返回null
     */
    public String login(String phone, String password) {
        // 获取配置中的默认用户信息
        String defaultPhone = UserConfigConstants.getDefaultUserPhone();
        String defaultPassword = UserConfigConstants.getDefaultUserPassword();
        String defaultEmail = UserConfigConstants.getDefaultUserEmail();

        // 验证用户名和密码
        if (StringUtils.equals(phone, defaultPhone) && StringUtils.equals(password, defaultPassword)) {
            // 创建用户对象
            User user = new User(phone, password, defaultEmail);
            
            // 生成Token
            String token = generateToken();
            user.setToken(token);
            
            // 保存Token到Redis
            saveUserToken(user, token);
            
            logger.info("用户登录成功: phone={}, token={}", phone, token);
            return token;
        }

        // 登录失败
        logger.warn("用户登录失败: phone={}", phone);
        return null;
    }

    /**
     * 用户登出
     * 清除Redis中的Token信息
     * 
     * @param token 用户Token
     */
    public void logout(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }

        // 根据Token获取手机号
        RBucket<String> tokenUserBucket = userRedissonClient.getBucket(TOKEN_USER_PREFIX + token);
        String userPhone = tokenUserBucket.get();
        
        if (StringUtils.isNotBlank(userPhone)) {
            // 删除用户Token映射
            RBucket<String> userTokenBucket = userRedissonClient.getBucket(USER_TOKEN_PREFIX + userPhone);
            userTokenBucket.delete();
            
            // 删除Token用户映射
            tokenUserBucket.delete();
            
            logger.info("用户登出成功: token={}", token);
        }
    }

    /**
     * 验证Token是否有效
     * 
     * @param token 用户Token
     * @return Token有效返回true，无效返回false
     */
    public boolean validateToken(String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }

        // 根据Token获取手机号
        RBucket<String> tokenUserBucket = userRedissonClient.getBucket(TOKEN_USER_PREFIX + token);
        String userPhone = tokenUserBucket.get();
        
        if (StringUtils.isBlank(userPhone)) {
            return false;
        }

        // 验证Token是否匹配
        RBucket<String> userTokenBucket = userRedissonClient.getBucket(USER_TOKEN_PREFIX + userPhone);
        String storedToken = userTokenBucket.get();
        
        return StringUtils.equals(token, storedToken);
    }

    /**
     * 根据Token获取用户信息
     * 
     * @param token 用户Token
     * @return 用户信息，Token无效返回null
     */
    public User getUserByToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        // 先验证Token是否有效
        if (!validateToken(token)) {
            return null;
        }

        // 根据Token获取手机号
        RBucket<String> tokenUserBucket = userRedissonClient.getBucket(TOKEN_USER_PREFIX + token);
        String userPhone = tokenUserBucket.get();
        
        if (StringUtils.isBlank(userPhone)) {
            return null;
        }

        // 构建用户对象
        User user = new User();
        user.setPhone(userPhone);
        user.setToken(token);
        user.setEmail(UserConfigConstants.getDefaultUserEmail());
        
        return user;
    }

    /**
     * 生成唯一Token
     * 使用UUID生成32位的随机字符串
     * 
     * @return 生成的Token
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 保存用户Token到Redis
     * 同时存储两个映射：
     * 1. 手机号 -> Token（用于查找用户当前的Token）
     * 2. Token -> 手机号（用于验证Token）
     * 
     * @param user 用户对象
     * @param token 生成的Token
     */
    private void saveUserToken(User user, String token) {
        // 获取Token过期时间（分钟）
        int expireMinutes = UserConfigConstants.getTokenExpireMinutes();

        // 先删除旧的Token映射
        RBucket<String> oldUserTokenBucket = userRedissonClient.getBucket(USER_TOKEN_PREFIX + user.getPhone());
        String oldToken = oldUserTokenBucket.get();
        
        if (StringUtils.isNotBlank(oldToken)) {
            RBucket<String> oldTokenUserBucket = userRedissonClient.getBucket(TOKEN_USER_PREFIX + oldToken);
            oldTokenUserBucket.delete();
        }

        // 存储新的Token映射
        // 手机号 -> Token
        RBucket<String> userTokenBucket = userRedissonClient.getBucket(USER_TOKEN_PREFIX + user.getPhone());
        userTokenBucket.set(token, expireMinutes, TimeUnit.MINUTES);
        
        // Token -> 手机号
        RBucket<String> tokenUserBucket = userRedissonClient.getBucket(TOKEN_USER_PREFIX + token);
        tokenUserBucket.set(user.getPhone(), expireMinutes, TimeUnit.MINUTES);
        
        logger.info("Token已保存到Redis, expireMinutes={}", expireMinutes);
    }
}
