package cn.keking.dao;

import java.sql.SQLException;

/**
 * 用户访问日志数据访问对象接口
 */
public interface UserAccessLogDao {

    /**
     * 初始化数据库表
     */
    void initDatabase() throws SQLException;

    /**
     * 插入访问日志
     * @param ipAddress IP地址
     * @param fileName 文件名
     * @param requestTime 请求时间
     * @param accessDate 访问日期
     * @throws SQLException SQL异常
     */
    void insert(String ipAddress, String fileName, String requestTime, String accessDate) throws SQLException;

    /**
     * 查询指定IP在指定日期的访问次数
     * @param ipAddress IP地址
     * @param accessDate 访问日期
     * @return 访问次数
     * @throws SQLException SQL异常
     */
    long countByIpAddressAndDate(String ipAddress, String accessDate) throws SQLException;
}
