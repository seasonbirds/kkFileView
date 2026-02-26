package cn.keking.service;

import cn.keking.model.UserAccessStats;
import cn.keking.model.UserBehaviorLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户行为监控服务接口
 * @author kkfileview
 */
public interface UserBehaviorService {

    /**
     * 记录用户请求
     * @param ipAddress IP地址
     * @param fileName 文件名
     * @param requestUrl 请求URL
     */
    void logRequest(String ipAddress, String fileName, String requestUrl);

    /**
     * 检查用户是否被限制访问（周期内频率限制）
     * @param ipAddress IP地址
     * @param windowMinutes 统计周期（分钟）
     * @param maxRequests 最大请求次数
     * @return 是否被限制
     */
    boolean isRateLimited(String ipAddress, int windowMinutes, int maxRequests);

    /**
     * 检查用户是否被限制访问（每日限制）
     * @param ipAddress IP地址
     * @param dailyMaxRequests 每日最大请求次数
     * @return 是否被限制
     */
    boolean isDailyLimited(String ipAddress, int dailyMaxRequests);

    /**
     * 获取周期内的请求次数
     * @param ipAddress IP地址
     * @param windowMinutes 统计周期（分钟）
     * @return 请求次数
     */
    int getRequestCountInWindow(String ipAddress, int windowMinutes);

    /**
     * 获取周期内的请求记录
     * @param ipAddress IP地址
     * @param windowMinutes 统计周期（分钟）
     * @return 请求记录列表
     */
    List<UserBehaviorLog> getRecentLogs(String ipAddress, int windowMinutes);

    /**
     * 增加告警次数
     * @param ipAddress IP地址
     */
    void incrementAlertCount(String ipAddress);

    /**
     * 清理过期数据
     * @param beforeTime 清理此时间之前的数据
     */
    void cleanupOldData(LocalDateTime beforeTime);

    /**
     * 获取用户当日统计
     * @param ipAddress IP地址
     * @return 统计信息
     */
    UserAccessStats getTodayStats(String ipAddress);
}
