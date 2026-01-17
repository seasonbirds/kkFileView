package cn.keking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * 用户访问计数服务
 */
@Service
public class UserAccessCountService {

    private static final Logger logger = LoggerFactory.getLogger(UserAccessCountService.class);

    @Autowired
    @Qualifier("userMonitorJedisPool")
    private JedisPool jedisPool;

    /**
     * 递增并获取计数
     * @param key 键
     * @return 计数值
     */
    public long incrementAndGet(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            long count = jedis.incr(key);
            if (count == 1) {
                jedis.expire(key, 86400);
            }
            return count;
        } catch (Exception e) {
            logger.error("Redis计数失败: key={}", key, e);
            throw e;
        }
    }

    /**
     * 获取计数
     * @param key 键
     * @return 计数值
     */
    public long get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key);
            return value == null ? 0 : Long.parseLong(value);
        } catch (Exception e) {
            logger.error("Redis获取计数失败: key={}", key, e);
            return 0;
        }
    }

    /**
     * 设置过期时间
     * @param key 键
     * @param seconds 秒数
     */
    public void expire(String key, int seconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.expire(key, seconds);
        } catch (Exception e) {
            logger.error("Redis设置过期时间失败: key={}", key, e);
        }
    }
}
