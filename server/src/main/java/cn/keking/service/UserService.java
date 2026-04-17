package cn.keking.service;

import cn.keking.config.UserConfigConstants;
import cn.keking.model.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static final String USER_TOKEN_PREFIX = "user:token:";
    private static final String TOKEN_USER_PREFIX = "token:user:";

    @Autowired(required = false)
    private StringRedisTemplate userStringRedisTemplate;

    private final Map<String, String> inMemoryUserTokenMap = new ConcurrentHashMap<>();
    private final Map<String, String> inMemoryTokenUserMap = new ConcurrentHashMap<>();
    private final Map<String, Long> inMemoryTokenExpireMap = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public UserService() {
        scheduler.scheduleAtFixedRate(this::cleanExpiredTokens, 1, 1, TimeUnit.MINUTES);
    }

    public String login(String phone, String password) {
        String defaultPhone = UserConfigConstants.getDefaultUserPhone();
        String defaultPassword = UserConfigConstants.getDefaultUserPassword();
        String defaultEmail = UserConfigConstants.getDefaultUserEmail();

        if (StringUtils.equals(phone, defaultPhone) && StringUtils.equals(password, defaultPassword)) {
            User user = new User(phone, password, defaultEmail);
            String token = generateToken();
            user.setToken(token);
            saveUserToken(user, token);
            logger.info("用户登录成功: phone={}, token={}", phone, token);
            return token;
        }

        logger.warn("用户登录失败: phone={}", phone);
        return null;
    }

    public void logout(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }

        if (useRedis()) {
            String userPhone = userStringRedisTemplate.opsForValue().get(TOKEN_USER_PREFIX + token);
            if (StringUtils.isNotBlank(userPhone)) {
                userStringRedisTemplate.delete(USER_TOKEN_PREFIX + userPhone);
                userStringRedisTemplate.delete(TOKEN_USER_PREFIX + token);
                logger.info("用户登出成功(Redis): token={}", token);
            }
        } else {
            String userPhone = inMemoryTokenUserMap.get(token);
            if (StringUtils.isNotBlank(userPhone)) {
                inMemoryUserTokenMap.remove(userPhone);
                inMemoryTokenUserMap.remove(token);
                inMemoryTokenExpireMap.remove(token);
                logger.info("用户登出成功(内存): token={}", token);
            }
        }
    }

    public boolean validateToken(String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }

        if (useRedis()) {
            String userPhone = userStringRedisTemplate.opsForValue().get(TOKEN_USER_PREFIX + token);
            if (StringUtils.isBlank(userPhone)) {
                return false;
            }

            String storedToken = userStringRedisTemplate.opsForValue().get(USER_TOKEN_PREFIX + userPhone);
            return StringUtils.equals(token, storedToken);
        } else {
            String userPhone = inMemoryTokenUserMap.get(token);
            if (StringUtils.isBlank(userPhone)) {
                return false;
            }

            Long expireTime = inMemoryTokenExpireMap.get(token);
            if (expireTime == null || System.currentTimeMillis() > expireTime) {
                inMemoryTokenUserMap.remove(token);
                inMemoryUserTokenMap.remove(userPhone);
                inMemoryTokenExpireMap.remove(token);
                return false;
            }

            String storedToken = inMemoryUserTokenMap.get(userPhone);
            return StringUtils.equals(token, storedToken);
        }
    }

    public User getUserByToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        if (!validateToken(token)) {
            return null;
        }

        String userPhone;
        if (useRedis()) {
            userPhone = userStringRedisTemplate.opsForValue().get(TOKEN_USER_PREFIX + token);
        } else {
            userPhone = inMemoryTokenUserMap.get(token);
        }

        if (StringUtils.isBlank(userPhone)) {
            return null;
        }

        User user = new User();
        user.setPhone(userPhone);
        user.setToken(token);
        user.setEmail(UserConfigConstants.getDefaultUserEmail());
        return user;
    }

    private boolean useRedis() {
        return userStringRedisTemplate != null;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void saveUserToken(User user, String token) {
        int expireMinutes = UserConfigConstants.getTokenExpireMinutes();

        if (useRedis()) {
            String oldToken = userStringRedisTemplate.opsForValue().get(USER_TOKEN_PREFIX + user.getPhone());
            if (StringUtils.isNotBlank(oldToken)) {
                userStringRedisTemplate.delete(TOKEN_USER_PREFIX + oldToken);
            }

            userStringRedisTemplate.opsForValue().set(USER_TOKEN_PREFIX + user.getPhone(), token, expireMinutes, TimeUnit.MINUTES);
            userStringRedisTemplate.opsForValue().set(TOKEN_USER_PREFIX + token, user.getPhone(), expireMinutes, TimeUnit.MINUTES);
            logger.info("Token已保存到Redis, expireMinutes={}", expireMinutes);
        } else {
            String oldToken = inMemoryUserTokenMap.get(user.getPhone());
            if (StringUtils.isNotBlank(oldToken)) {
                inMemoryTokenUserMap.remove(oldToken);
                inMemoryTokenExpireMap.remove(oldToken);
            }

            inMemoryUserTokenMap.put(user.getPhone(), token);
            inMemoryTokenUserMap.put(token, user.getPhone());
            inMemoryTokenExpireMap.put(token, System.currentTimeMillis() + expireMinutes * 60 * 1000L);
            logger.info("Token已保存到内存, expireMinutes={}", expireMinutes);
        }
    }

    private void cleanExpiredTokens() {
        if (useRedis()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : inMemoryTokenExpireMap.entrySet()) {
            if (entry.getValue() < now) {
                String token = entry.getKey();
                String userPhone = inMemoryTokenUserMap.get(token);
                if (userPhone != null) {
                    inMemoryUserTokenMap.remove(userPhone);
                }
                inMemoryTokenUserMap.remove(token);
                inMemoryTokenExpireMap.remove(token);
                logger.info("已清理过期Token: token={}", token);
            }
        }
    }
}
