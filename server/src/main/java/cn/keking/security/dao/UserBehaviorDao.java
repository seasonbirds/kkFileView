package cn.keking.security.dao;

import cn.keking.security.model.UserBehaviorLog;
import cn.keking.security.config.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户行为数据访问对象
 * 负责与SQLite数据库交互，实现用户行为日志的持久化存储和查询
 * 提供线程安全的数据库操作
 */
@Repository
public class UserBehaviorDao {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorDao.class);

    /**
     * 创建用户行为日志表的SQL语句
     * 使用CREATE TABLE IF NOT EXISTS避免数据丢失
     */
    private static final String CREATE_BEHAVIOR_LOG_TABLE = 
        "CREATE TABLE IF NOT EXISTS user_behavior_log (" +
        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
        "ip_address TEXT NOT NULL, " +
        "file_name TEXT, " +
        "request_time TIMESTAMP NOT NULL)";
    
    /**
     * 安全配置对象，包含数据库路径等配置
     */
    private final SecurityConfig securityConfig;

    /**
     * 数据库连接对象，使用volatile保证线程可见性
     */
    private volatile Connection connection;

    public UserBehaviorDao(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
        initDatabase();
    }

    /**
     * 初始化数据库连接并创建必要的表
     * 使用synchronized保证线程安全的单例初始化
     */
    private synchronized void initDatabase() {
        if (connection != null) {
            return;
        }
        try {
            Class.forName("org.sqlite.JDBC");
            String jdbcUrl = "jdbc:sqlite:" + securityConfig.getDatabasePath();
            this.connection = DriverManager.getConnection(jdbcUrl);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(CREATE_BEHAVIOR_LOG_TABLE);
            }
            logger.info("User behavior database initialized successfully");
        } catch (ClassNotFoundException e) {
            logger.error("SQLite JDBC driver not found", e);
        } catch (SQLException e) {
            logger.error("Failed to initialize user behavior database", e);
        }
    }

    /**
     * 获取数据库连接
     * 连接不存在或已关闭时自动重新初始化
     * @return 数据库连接对象
     * @throws SQLException 获取连接失败时抛出
     */
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initDatabase();
        }
        return connection;
    }

    /**
     * 插入用户行为日志
     * @param log 用户行为日志对象
     */
    public void insertBehaviorLog(UserBehaviorLog log) {
        String sql = "INSERT INTO user_behavior_log (ip_address, file_name, request_time) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, log.getIpAddress());
            pstmt.setString(2, log.getFileName());
            pstmt.setTimestamp(3, Timestamp.valueOf(log.getRequestTime()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to insert user behavior log", e);
        }
    }

    /**
     * 统计指定IP在指定时间范围内的请求次数
     * @param ipAddress IP地址
     * @param startTime 统计开始时间
     * @return 请求次数
     */
    public int countRequestsInPeriod(String ipAddress, LocalDateTime startTime) {
        String sql = "SELECT COUNT(*) FROM user_behavior_log WHERE ip_address = ? AND request_time >= ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setTimestamp(2, Timestamp.valueOf(startTime));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to count requests in period", e);
        }
        return 0;
    }

    /**
     * 清理指定天数之前的历史数据
     * @param daysToKeep 保留的天数
     */
    public void cleanupOldData(int daysToKeep) {
        String deleteLogSql = "DELETE FROM user_behavior_log WHERE request_time < DATE('now', '-? days')";

        try (Connection conn = getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(deleteLogSql)) {
                pstmt.setInt(1, daysToKeep);
                int deleted = pstmt.executeUpdate();
                logger.info("Cleaned up {} old behavior log records", deleted);
            }
        } catch (SQLException e) {
            logger.error("Failed to cleanup old data", e);
        }
    }

    /**
     * 根据IP地址查询用户行为日志
     * @param ipAddress IP地址
     * @param limit 返回记录的最大数量
     * @return 用户行为日志列表
     */
    public List<UserBehaviorLog> getLogsByIp(String ipAddress, int limit) {
        String sql = "SELECT * FROM user_behavior_log WHERE ip_address = ? ORDER BY request_time DESC LIMIT ?";
        List<UserBehaviorLog> logs = new ArrayList<>();
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UserBehaviorLog log = new UserBehaviorLog();
                    log.setId(rs.getLong("id"));
                    log.setIpAddress(rs.getString("ip_address"));
                    log.setFileName(rs.getString("file_name"));
                    log.setRequestTime(rs.getTimestamp("request_time").toLocalDateTime());
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get logs by IP", e);
        }
        return logs;
    }
}
