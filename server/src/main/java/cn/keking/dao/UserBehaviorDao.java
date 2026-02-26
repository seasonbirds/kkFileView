package cn.keking.dao;

import cn.keking.model.UserAccessStats;
import cn.keking.model.UserBehaviorLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户行为数据访问接口
 * @author kkfileview
 */
public interface UserBehaviorDao {

    /**
     * 插入行为日志
     * @param log 日志对象
     */
    void insertLog(UserBehaviorLog log);

    /**
     * 获取周期内的请求次数
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @return 请求次数
     */
    int getRequestCount(String ipAddress, LocalDateTime startTime);

    /**
     * 获取周期内的请求记录
     * @param ipAddress IP地址
     * @param startTime 开始时间
     * @return 请求记录列表
     */
    List<UserBehaviorLog> getRecentLogs(String ipAddress, LocalDateTime startTime);

    /**
     * 获取或创建当日统计
     * @param ipAddress IP地址
     * @param today 日期
     * @return 统计对象
     */
    UserAccessStats getOrCreateTodayStats(String ipAddress, String today);

    /**
     * 更新每日请求计数
     * @param ipAddress IP地址
     * @param today 日期
     * @param increment 增加数量
     */
    void incrementDailyCount(String ipAddress, String today, int increment);

    /**
     * 增加告警次数
     * @param ipAddress IP地址
     * @param today 日期
     */
    void incrementAlertCount(String ipAddress, String today);

    /**
     * 设置阻止状态
     * @param ipAddress IP地址
     * @param today 日期
     * @param blocked 是否阻止
     */
    void setBlocked(String ipAddress, String today, boolean blocked);

    /**
     * 清理过期数据
     * @param beforeTime 截止时间
     */
    void cleanupOldData(LocalDateTime beforeTime);
}
