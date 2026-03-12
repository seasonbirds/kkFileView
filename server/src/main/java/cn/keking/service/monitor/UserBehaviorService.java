package cn.keking.service.monitor;

import cn.keking.config.ConfigConstants;
import cn.keking.model.monitor.BlockedIp;
import cn.keking.model.monitor.UserBehavior;
import cn.keking.repository.monitor.BlockedIpRepository;
import cn.keking.repository.monitor.UserBehaviorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * 用户行为监控服务
 * 负责记录用户访问行为和访问频率控制
 * 所有数据库操作都使用异步线程池，避免影响正常请求
 */
@Service
public class UserBehaviorService {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorService.class);

    private final UserBehaviorRepository userBehaviorRepository;
    private final BlockedIpRepository blockedIpRepository;
    private final EmailAlertService emailAlertService;
    private final AccessCounter accessCounter;

    public UserBehaviorService(UserBehaviorRepository userBehaviorRepository,
                               BlockedIpRepository blockedIpRepository,
                               EmailAlertService emailAlertService,
                               AccessCounter accessCounter) {
        this.userBehaviorRepository = userBehaviorRepository;
        this.blockedIpRepository = blockedIpRepository;
        this.emailAlertService = emailAlertService;
        this.accessCounter = accessCounter;
    }

    /**
     * 异步记录用户行为
     * @param ipAddress IP地址
     * @param fileName 文件名
     * @param requestUrl 请求URL
     * @param userAgent 用户代理
     * @param blocked 是否被拦截
     */
    @Async("monitorTaskExecutor")
    @Transactional
    public void recordUserBehavior(String ipAddress, String fileName, String requestUrl, String userAgent, boolean blocked) {
        try {
            UserBehavior behavior = new UserBehavior(ipAddress, fileName, requestUrl, userAgent);
            behavior.setBlocked(blocked);
            userBehaviorRepository.save(behavior);
            logger.debug("记录用户行为: IP={}, 文件名={}, 被拦截={}", ipAddress, fileName, blocked);
        } catch (Exception e) {
            logger.error("记录用户行为失败", e);
        }
    }

    /**
     * 封禁IP地址（先内存后数据库）
     * @param ipAddress IP地址
     * @param reason 封禁原因
     * @param isDailyLimit 是否是每日限额
     */
    public void blockIp(String ipAddress, String reason, boolean isDailyLimit) {
        if (!ConfigConstants.getMonitorEnabled()) {
            return;
        }
        
        // 1. 计算封禁结束时间
        int blockDuration = ConfigConstants.getMonitorBlockDurationMinutes();
        LocalDateTime blockEndTime;

        if (isDailyLimit) {
            blockEndTime = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN);
        } else {
            blockEndTime = LocalDateTime.now().plusMinutes(blockDuration);
        }

        // 2. 先更新内存缓存（立即生效）
        accessCounter.markIpBlocked(ipAddress, blockEndTime, reason);
        logger.warn("IP {} 被封禁（内存）, 原因: {}, 解封时间: {}", ipAddress, reason, blockEndTime);

        // 3. 异步更新数据库
        asyncBlockIpToDatabase(ipAddress, reason, isDailyLimit, blockEndTime);
    }

    /**
     * 异步写入封禁信息到数据库
     */
    @Async("monitorTaskExecutor")
    @Transactional
    protected void asyncBlockIpToDatabase(String ipAddress, String reason, boolean isDailyLimit, LocalDateTime blockEndTime) {
        try {
            Optional<BlockedIp> existingBlock = blockedIpRepository.findByIpAddress(ipAddress);
            BlockedIp blockedIp;

            if (existingBlock.isPresent()) {
                blockedIp = existingBlock.get();
                blockedIp.setBlockEndTime(blockEndTime);
                blockedIp.setReason(reason);
                blockedIp.setIsDailyLimit(isDailyLimit);
            } else {
                blockedIp = new BlockedIp(ipAddress, blockEndTime, reason, isDailyLimit);
            }

            blockedIpRepository.save(blockedIp);
            logger.debug("IP {} 封禁信息写入数据库完成", ipAddress);

            // 发送告警邮件
            if (ConfigConstants.getMonitorAlertEmailEnabled()) {
                emailAlertService.sendBlockAlert(ipAddress, reason, blockEndTime);
            }
        } catch (Exception e) {
            logger.error("封禁IP写入数据库失败: {}", ipAddress, e);
        }
    }

    /**
     * 重新从数据库加载封禁IP列表（用于定时同步）
     */
    @Async("monitorTaskExecutor")
    public void reloadBlockedIps() {
        try {
            accessCounter.loadBlockedIpsFromDatabase();
        } catch (Exception e) {
            logger.error("重新加载封禁IP列表失败", e);
        }
    }
}
