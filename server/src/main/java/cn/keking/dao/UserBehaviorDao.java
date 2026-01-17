package cn.keking.dao;

import cn.keking.model.UserBehaviorLog;

import java.util.List;

/**
 * 用户行为日志DAO接口
 *
 * @author keking
 * @since 2025-07-17
 */
public interface UserBehaviorDao {

    /**
     * 保存用户行为日志
     *
     * @param log 用户行为日志
     */
    void save(UserBehaviorLog log);

    /**
     * 批量保存用户行为日志
     *
     * @param logs 用户行为日志列表
     */
    void batchSave(List<UserBehaviorLog> logs);

    /**
     * 获取指定IP在指定时间段内的请求次数
     *
     * @param ipAddress IP地址
     * @param minutes 分钟数
     * @return 请求次数
     */
    int getRequestCountInPeriod(String ipAddress, int minutes);

    /**
     * 获取指定IP当天的请求次数
     *
     * @param ipAddress IP地址
     * @return 请求次数
     */
    int getRequestCountToday(String ipAddress);

    /**
     * 获取指定IP的行为日志列表
     *
     * @param ipAddress IP地址
     * @param limit 返回的最大记录数
     * @return 行为日志列表
     */
    List<UserBehaviorLog> getLogsByIp(String ipAddress, int limit);
}
