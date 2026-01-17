package cn.keking.dao.impl;

import cn.keking.dao.UserAccessLogDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.*;

@Repository
public class UserAccessLogDaoImpl implements UserAccessLogDao {

    private static final Logger logger = LoggerFactory.getLogger(UserAccessLogDaoImpl.class);

    @Value("${user.monitor.db.path:./data/user_monitor.db}")
    private String dbPath;

    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS user_access_log (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "ip_address TEXT NOT NULL, " +
            "file_name TEXT NOT NULL, " +
            "request_time TEXT NOT NULL, " +
            "access_date TEXT NOT NULL)";

    private static final String INSERT_SQL = "INSERT INTO user_access_log (ip_address, file_name, request_time, access_date) VALUES (?, ?, ?, ?)";

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM user_access_log WHERE ip_address = ? AND access_date = ?";

    @Override
    public void initDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
            logger.info("用户访问日志数据库初始化成功: {}", dbPath);
        } catch (SQLException e) {
            logger.error("初始化用户访问日志数据库失败", e);
            throw e;
        }
    }

    @Override
    public void insert(String ipAddress, String fileName, String requestTime, String accessDate) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, fileName);
            pstmt.setString(3, requestTime);
            pstmt.setString(4, accessDate);
            pstmt.executeUpdate();
            logger.debug("记录用户访问: IP={}, 文件={}, 时间={}", ipAddress, fileName, requestTime);
        } catch (SQLException e) {
            logger.error("记录用户访问失败: IP={}, 文件={}", ipAddress, fileName, e);
            throw e;
        }
    }

    @Override
    public long countByIpAddressAndDate(String ipAddress, String accessDate) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             PreparedStatement pstmt = conn.prepareStatement(COUNT_SQL)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, accessDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            logger.error("查询每日访问次数失败: IP={}, 日期={}", ipAddress, accessDate, e);
            throw e;
        }
        return 0;
    }
}
