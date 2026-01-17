package cn.keking.service;

import cn.keking.dao.UserAccessLogDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户访问日志服务
 */
@Service
public class UserAccessLogService {

    private static final Logger logger = LoggerFactory.getLogger(UserAccessLogService.class);

    private final UserAccessLogDao userAccessLogDao;

    @Autowired
    public UserAccessLogService(UserAccessLogDao userAccessLogDao) {
        this.userAccessLogDao = userAccessLogDao;
    }

    /**
     * 初始化数据库
     */
    public void initDatabase() {
        try {
            userAccessLogDao.initDatabase();
        } catch (Exception e) {
            logger.error("初始化用户访问日志数据库失败", e);
        }
    }

    /**
     * 异步记录访问日志
     * @param ipAddress IP地址
     * @param fileName 文件名
     */
    @Async("taskExecutor")
    public void recordAccessAsync(String ipAddress, String fileName) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String requestTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String accessDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            userAccessLogDao.insert(ipAddress, fileName, requestTime, accessDate);
            logger.debug("记录用户访问: IP={}, 文件={}, 时间={}", ipAddress, fileName, requestTime);
        } catch (Exception e) {
            logger.error("异步记录用户访问失败: IP={}, 文件={}", ipAddress, fileName, e);
        }
    }

    /**
     * 获取每日访问次数
     * @param ipAddress IP地址
     * @return 访问次数
     */
    public long getDailyAccessCount(String ipAddress) {
        try {
            String today = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return userAccessLogDao.countByIpAddressAndDate(ipAddress, today);
        } catch (Exception e) {
            logger.error("查询每日访问次数失败: IP={}", ipAddress, e);
            return 0;
        }
    }
}
