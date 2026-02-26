package cn.keking.behavior.service;

import cn.keking.behavior.cache.BehaviorCacheManager;
import cn.keking.behavior.config.BehaviorConfig;
import cn.keking.behavior.dao.RequestLogDao;
import cn.keking.behavior.model.RequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 请求日志服务类
 * 负责请求日志的记录和访问频率控制
 */
@Service
public class RequestLogService {

    private static final Logger logger = LoggerFactory.getLogger(RequestLogService.class);

    private final RequestLogDao requestLogDao;
    private final BehaviorCacheManager cacheManager;

    public RequestLogService(RequestLogDao requestLogDao, BehaviorCacheManager cacheManager) {
        this.requestLogDao = requestLogDao;
        this.cacheManager = cacheManager;
    }

    /**
     * 异步保存请求日志到数据库
     * @param requestLog 请求日志对象
     */
    @Async("behaviorTaskExecutor")
    public void saveRequestLogAsync(RequestLog requestLog) {
        try {
            requestLogDao.save(requestLog);
        } catch (Exception e) {
            logger.error("Failed to save request log asynchronously", e);
        }
    }

    /**
     * 记录并检查IP访问
     * 此方法会增加计数并返回当前计数
     * @param ipAddress IP地址
     * @return 当前周期内的访问次数
     */
    public int recordAndGetPeriodCount(String ipAddress) {
        return cacheManager.incrementPeriodCount(ipAddress);
    }

    /**
     * 记录并检查IP每日访问
     * @param ipAddress IP地址
     * @return 当天的访问次数
     */
    public int recordAndGetDailyCount(String ipAddress) {
        return cacheManager.incrementDailyCount(ipAddress);
    }

    /**
     * 检查周期内访问是否超过阈值
     * @param ipAddress IP地址
     * @return 是否超过阈值
     */
    public boolean isPeriodThresholdExceeded(String ipAddress) {
        return cacheManager.isPeriodThresholdExceeded(ipAddress);
    }

    /**
     * 检查每日访问是否超过阈值
     * @param ipAddress IP地址
     * @return 是否超过阈值
     */
    public boolean isDailyThresholdExceeded(String ipAddress) {
        return cacheManager.isDailyThresholdExceeded(ipAddress);
    }

    /**
     * 获取周期内访问计数
     * @param ipAddress IP地址
     * @return 访问计数
     */
    public int getPeriodCount(String ipAddress) {
        return cacheManager.getPeriodCount(ipAddress);
    }

    /**
     * 获取每日访问计数
     * @param ipAddress IP地址
     * @return 访问计数
     */
    public int getDailyCount(String ipAddress) {
        return cacheManager.getDailyCount(ipAddress);
    }

    /**
     * 检查是否应该发送周期告警
     * @param ipAddress IP地址
     * @return 是否应该发送
     */
    public boolean shouldSendPeriodAlert(String ipAddress) {
        return cacheManager.shouldSendPeriodAlert(ipAddress);
    }

    /**
     * 检查是否应该发送每日告警
     * @param ipAddress IP地址
     * @return 是否应该发送
     */
    public boolean shouldSendDailyAlert(String ipAddress) {
        return cacheManager.shouldSendDailyAlert(ipAddress);
    }

    /**
     * 获取当前统计周期开始时间
     * @return 周期开始时间
     */
    public LocalDateTime getPeriodStartTime() {
        return cacheManager.getPeriodStartTime();
    }
}
