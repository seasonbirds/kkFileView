package cn.keking.service;

import cn.keking.model.UserBehavior;
import cn.keking.repository.UserBehaviorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 用户行为服务类
 */
@Service
public class UserBehaviorService {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorService.class);

    @Autowired
    private UserBehaviorRepository userBehaviorRepository;

    /**
     * 异步记录用户行为
     */
    @Async("userBehaviorExecutor")
    public CompletableFuture<Void> recordUserBehavior(String ipAddress, String fileUrl, String userAgent) {
        try {
            UserBehavior userBehavior = new UserBehavior(ipAddress, fileUrl, LocalDateTime.now(), userAgent);
            userBehaviorRepository.save(userBehavior);
            logger.debug("记录用户行为: IP={}, 文件={}", ipAddress, fileUrl);
        } catch (Exception e) {
            logger.error("记录用户行为时发生错误", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 检查用户在指定时间窗口内的访问次数是否超过阈值
     */
    public boolean isAccessExceeded(String ipAddress, int timeWindowMinutes, int maxAccessCount) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.minusMinutes(timeWindowMinutes);
            
            long accessCount = userBehaviorRepository.countByIpAddressAndRequestTimeBetween(ipAddress, startTime, now);
            logger.debug("IP {} 在过去 {} 分钟内访问了 {} 次", ipAddress, timeWindowMinutes, accessCount);
            
            return accessCount > maxAccessCount;
        } catch (Exception e) {
            logger.error("检查访问次数时发生错误", e);
            return false; // 出错时不阻止用户访问
        }
    }

    /**
     * 检查用户在当天的访问次数是否超过阈值
     */
    public boolean isDailyAccessExceeded(String ipAddress, int maxDailyAccessCount) {
        try {
            LocalDateTime now = LocalDateTime.now();
            long accessCount = userBehaviorRepository.countByIpAddressAndDate(ipAddress, now);
            logger.debug("IP {} 在今天已访问 {} 次", ipAddress, accessCount);
            
            return accessCount > maxDailyAccessCount;
        } catch (Exception e) {
            logger.error("检查每日访问次数时发生错误", e);
            return false; // 出错时不阻止用户访问
        }
    }

    /**
     * 异步标记用户行为为异常
     */
    @Async("userBehaviorExecutor")
    public CompletableFuture<Void> markBehaviorAsAbnormal(String ipAddress, String description) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.minusMinutes(5); // 获取最近5分钟内的记录
            
            List<UserBehavior> behaviors = userBehaviorRepository.findByIpAddressAndRequestTimeBetween(ipAddress, startTime, now);
            for (UserBehavior behavior : behaviors) {
                behavior.setIsAbnormal(true);
                behavior.setAbnormalDescription(description);
                userBehaviorRepository.save(behavior);
            }
            
            logger.warn("标记IP {} 的行为为异常: {}", ipAddress, description);
        } catch (Exception e) {
            logger.error("标记用户行为为异常时发生错误", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 获取用户在指定时间窗口内的访问记录
     */
    public List<UserBehavior> getUserBehaviors(String ipAddress, int timeWindowMinutes) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.minusMinutes(timeWindowMinutes);
            
            return userBehaviorRepository.findByIpAddressAndRequestTimeBetween(ipAddress, startTime, now);
        } catch (Exception e) {
            logger.error("获取用户行为记录时发生错误", e);
            return null;
        }
    }

    /**
     * 获取所有异常行为记录
     */
    public List<UserBehavior> getAllAbnormalBehaviors() {
        try {
            return userBehaviorRepository.findByIsAbnormalTrue();
        } catch (Exception e) {
            logger.error("获取异常行为记录时发生错误", e);
            return null;
        }
    }
}