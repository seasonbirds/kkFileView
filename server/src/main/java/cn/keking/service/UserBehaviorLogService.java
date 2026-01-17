package cn.keking.service;

import cn.keking.dao.UserBehaviorDao;
import cn.keking.dao.impl.UserBehaviorDaoImpl;
import cn.keking.model.UserBehaviorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户行为日志服务
 * 负责用户行为日志的持久化操作
 *
 * @author keking
 * @since 2025-07-17
 */
@Service
public class UserBehaviorLogService {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorLogService.class);
    private final UserBehaviorDao userBehaviorDao;

    public UserBehaviorLogService() {
        this.userBehaviorDao = new UserBehaviorDaoImpl();
    }

    /**
     * 保存用户行为日志
     *
     * @param log 用户行为日志
     */
    public void saveLog(UserBehaviorLog log) {
        userBehaviorDao.save(log);
    }

    /**
     * 批量保存用户行为日志
     *
     * @param logs 用户行为日志列表
     */
    public void batchSaveLogs(List<UserBehaviorLog> logs) {
        userBehaviorDao.batchSave(logs);
    }

    /**
     * 获取指定IP在指定时间段内的请求次数
     *
     * @param ipAddress IP地址
     * @param minutes 分钟数
     * @return 请求次数
     */
    public int getRequestCountInPeriod(String ipAddress, int minutes) {
        return userBehaviorDao.getRequestCountInPeriod(ipAddress, minutes);
    }

    /**
     * 获取指定IP当天的请求次数
     *
     * @param ipAddress IP地址
     * @return 请求次数
     */
    public int getRequestCountToday(String ipAddress) {
        return userBehaviorDao.getRequestCountToday(ipAddress);
    }

    /**
     * 获取指定IP的行为日志列表
     *
     * @param ipAddress IP地址
     * @param limit 返回的最大记录数
     * @return 行为日志列表
     */
    public List<UserBehaviorLog> getLogsByIp(String ipAddress, int limit) {
        return userBehaviorDao.getLogsByIp(ipAddress, limit);
    }
}
