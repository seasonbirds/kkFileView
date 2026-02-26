package cn.keking.dao.impl;

import cn.keking.dao.UserBehaviorDao;
import cn.keking.model.UserAccessStats;
import cn.keking.model.UserBehaviorLog;
import cn.keking.utils.ConfigUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户行为数据访问实现类
 * @author kkfileview
 */
@Repository
public class UserBehaviorDaoImpl implements UserBehaviorDao {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorDaoImpl.class);
    private static final String DB_NAME = "user_behavior.db";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Connection connection;

    @PostConstruct
    public void init() {
        try {
            initializeDatabase();
            logger.info("用户行为监控数据库初始化成功");
        } catch (Exception e) {
            logger.error("用户行为监控数据库初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("用户行为监控数据库连接已关闭");
            }
        } catch (SQLException e) {
            logger.error("关闭数据库连接失败", e);
        }
    }

    private void initializeDatabase() throws SQLException {
        String dbPath = ConfigUtils.getHomePath() + File.separator + "db" + File.separator + DB_NAME;
        File dbDir = new File(ConfigUtils.getHomePath() + File.separator + "db");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        String url = "jdbc:sqlite:" + dbPath;
        connection = DriverManager.getConnection(url);
        connection.setAutoCommit(true);

        createTables();
    }

    private void createTables() throws SQLException {
        String createBehaviorLogTable = "CREATE TABLE IF NOT EXISTS user_behavior_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ip_address TEXT NOT NULL," +
                "file_name TEXT," +
                "request_time TIMESTAMP NOT NULL," +
                "request_url TEXT" +
                ")";

        String createAccessStatsTable = "CREATE TABLE IF NOT EXISTS user_access_stats (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ip_address TEXT NOT NULL," +
                "access_date TEXT NOT NULL," +
                "request_count INTEGER DEFAULT 0," +
                "alert_count INTEGER DEFAULT 0," +
                "first_access_time TIMESTAMP," +
                "last_access_time TIMESTAMP," +
                "blocked INTEGER DEFAULT 0," +
                "UNIQUE(ip_address, access_date)" +
                ")";

        String createIndex1 = "CREATE INDEX IF NOT EXISTS idx_behavior_log_ip_time ON user_behavior_log(ip_address, request_time)";
        String createIndex2 = "CREATE INDEX IF NOT EXISTS idx_behavior_log_time ON user_behavior_log(request_time)";
        String createIndex3 = "CREATE INDEX IF NOT EXISTS idx_access_stats_ip_date ON user_access_stats(ip_address, access_date)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createBehaviorLogTable);
            stmt.execute(createAccessStatsTable);
            stmt.execute(createIndex1);
            stmt.execute(createIndex2);
            stmt.execute(createIndex3);
        }
    }

    @Override
    public void insertLog(UserBehaviorLog log) {
        if (connection == null) {
            return;
        }

        String sql = "INSERT INTO user_behavior_log (ip_address, file_name, request_time, request_url) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, log.getIpAddress());
            pstmt.setString(2, log.getFileName());
            pstmt.setString(3, log.getRequestTime().format(DATETIME_FORMATTER));
            pstmt.setString(4, log.getRequestUrl());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("记录用户请求失败", e);
        }
    }

    @Override
    public int getRequestCount(String ipAddress, LocalDateTime startTime) {
        if (connection == null) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM user_behavior_log WHERE ip_address = ? AND request_time > ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, startTime.format(DATETIME_FORMATTER));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("获取请求次数失败", e);
        }
        return 0;
    }

    @Override
    public List<UserBehaviorLog> getRecentLogs(String ipAddress, LocalDateTime startTime) {
        List<UserBehaviorLog> logs = new ArrayList<>();
        if (connection == null) {
            return logs;
        }

        String sql = "SELECT * FROM user_behavior_log WHERE ip_address = ? AND request_time > ? ORDER BY request_time DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, startTime.format(DATETIME_FORMATTER));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                UserBehaviorLog log = new UserBehaviorLog();
                log.setId(rs.getLong("id"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setFileName(rs.getString("file_name"));
                log.setRequestTime(LocalDateTime.parse(rs.getString("request_time"), DATETIME_FORMATTER));
                log.setRequestUrl(rs.getString("request_url"));
                logs.add(log);
            }
        } catch (SQLException e) {
            logger.error("获取近期日志失败", e);
        }
        return logs;
    }

    @Override
    public UserAccessStats getOrCreateTodayStats(String ipAddress, String today) {
        if (connection == null) {
            return null;
        }

        String selectSql = "SELECT * FROM user_access_stats WHERE ip_address = ? AND access_date = ?";
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setString(1, ipAddress);
            selectStmt.setString(2, today);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToStats(rs);
            } else {
                String insertSql = "INSERT INTO user_access_stats (ip_address, access_date, request_count, first_access_time, last_access_time, blocked) VALUES (?, ?, 0, ?, ?, 0)";
                LocalDateTime now = LocalDateTime.now();
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, ipAddress);
                    insertStmt.setString(2, today);
                    insertStmt.setString(3, now.format(DATETIME_FORMATTER));
                    insertStmt.setString(4, now.format(DATETIME_FORMATTER));
                    insertStmt.executeUpdate();
                }
                return getOrCreateTodayStats(ipAddress, today);
            }
        } catch (SQLException e) {
            logger.error("获取或创建今日统计失败", e);
        }
        return null;
    }

    @Override
    public void incrementDailyCount(String ipAddress, String today, int increment) {
        if (connection == null) {
            return;
        }

        String sql = "UPDATE user_access_stats SET request_count = request_count + ?, last_access_time = ? WHERE ip_address = ? AND access_date = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, increment);
            pstmt.setString(2, LocalDateTime.now().format(DATETIME_FORMATTER));
            pstmt.setString(3, ipAddress);
            pstmt.setString(4, today);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("增加每日计数失败", e);
        }
    }

    @Override
    public void incrementAlertCount(String ipAddress, String today) {
        if (connection == null) {
            return;
        }

        String sql = "UPDATE user_access_stats SET alert_count = alert_count + 1 WHERE ip_address = ? AND access_date = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, today);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("增加告警次数失败", e);
        }
    }

    @Override
    public void setBlocked(String ipAddress, String today, boolean blocked) {
        if (connection == null) {
            return;
        }

        String sql = "UPDATE user_access_stats SET blocked = ? WHERE ip_address = ? AND access_date = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, blocked ? 1 : 0);
            pstmt.setString(2, ipAddress);
            pstmt.setString(3, today);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("设置阻止状态失败", e);
        }
    }

    @Override
    public void cleanupOldData(LocalDateTime beforeTime) {
        if (connection == null) {
            return;
        }

        String sql = "DELETE FROM user_behavior_log WHERE request_time < ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, beforeTime.format(DATETIME_FORMATTER));
            int deleted = pstmt.executeUpdate();
            logger.info("清理用户行为日志 {} 条", deleted);
        } catch (SQLException e) {
            logger.error("清理过期数据失败", e);
        }
    }

    private UserAccessStats mapResultSetToStats(ResultSet rs) throws SQLException {
        UserAccessStats stats = new UserAccessStats();
        stats.setId(rs.getLong("id"));
        stats.setIpAddress(rs.getString("ip_address"));
        stats.setAccessDate(LocalDate.parse(rs.getString("access_date"), DATE_FORMATTER));
        stats.setRequestCount(rs.getInt("request_count"));
        stats.setAlertCount(rs.getInt("alert_count"));
        String firstAccess = rs.getString("first_access_time");
        if (firstAccess != null) {
            stats.setFirstAccessTime(LocalDateTime.parse(firstAccess, DATETIME_FORMATTER));
        }
        String lastAccess = rs.getString("last_access_time");
        if (lastAccess != null) {
            stats.setLastAccessTime(LocalDateTime.parse(lastAccess, DATETIME_FORMATTER));
        }
        stats.setBlocked(rs.getInt("blocked") == 1);
        return stats;
    }
}
