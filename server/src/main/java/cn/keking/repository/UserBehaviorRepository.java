package cn.keking.repository;

import cn.keking.config.ConfigConstants;
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

/**
 * 用户行为数据访问层
 * 负责SQLite数据库的所有操作
 *
 * @author kkFileView
 */
@Repository
public class UserBehaviorRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorRepository.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Connection connection;

    @PostConstruct
    public void init() {
        if (!ConfigConstants.isUserBehaviorMonitorEnabled()) {
            logger.info("用户行为监控功能未启用，跳过数据库初始化");
            return;
        }
        try {
            initDatabase();
            logger.info("用户行为监控数据库初始化完成");
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

    /**
     * 初始化SQLite数据库
     */
    private void initDatabase() throws SQLException {
        String dbPath = ConfigConstants.getUserBehaviorDbPath();
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        String url = "jdbc:sqlite:" + dbPath;
        connection = DriverManager.getConnection(url);

        // 创建访问记录表
        String createAccessLogTable = "CREATE TABLE IF NOT EXISTS access_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ip_address TEXT NOT NULL," +
                "file_name TEXT," +
                "request_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "request_url TEXT," +
                "user_agent TEXT" +
                ")";

        // 创建每日访问统计表
        String createDailyStatsTable = "CREATE TABLE IF NOT EXISTS daily_access_stats (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ip_address TEXT NOT NULL," +
                "access_date TEXT NOT NULL," +
                "access_count INTEGER DEFAULT 0," +
                "UNIQUE(ip_address, access_date)" +
                ")";

        // 创建告警记录表
        String createAlertLogTable = "CREATE TABLE IF NOT EXISTS alert_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ip_address TEXT NOT NULL," +
                "alert_type TEXT NOT NULL," +
                "alert_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "access_count INTEGER," +
                "threshold INTEGER," +
                "alert_message TEXT" +
                ")";

        // 创建索引
        String createAccessLogIndex = "CREATE INDEX IF NOT EXISTS idx_access_log_ip_time ON access_log(ip_address, request_time)";
        String createDailyStatsIndex = "CREATE INDEX IF NOT EXISTS idx_daily_stats_ip_date ON daily_access_stats(ip_address, access_date)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createAccessLogTable);
            stmt.execute(createDailyStatsTable);
            stmt.execute(createAlertLogTable);
            stmt.execute(createAccessLogIndex);
            stmt.execute(createDailyStatsIndex);
        }

        logger.info("SQLite数据库初始化完成: {}", dbPath);
    }

    /**
     * 插入访问记录
     */
    public void insertAccessLog(String ipAddress, String fileName, String requestUrl, String userAgent) throws SQLException {
        String sql = "INSERT INTO access_log (ip_address, file_name, request_url, user_agent) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, fileName);
            pstmt.setString(3, requestUrl);
            pstmt.setString(4, userAgent);
            pstmt.executeUpdate();
        }
    }

    /**
     * 更新每日访问统计
     */
    public void updateDailyStats(String ipAddress, String today) throws SQLException {
        String sql = "INSERT INTO daily_access_stats (ip_address, access_date, access_count) " +
                "VALUES (?, ?, 1) " +
                "ON CONFLICT(ip_address, access_date) DO UPDATE SET access_count = access_count + 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, today);
            pstmt.executeUpdate();
        }
    }

    /**
     * 查询每日访问次数
     */
    public int getDailyAccessCount(String ipAddress, String today) throws SQLException {
        String sql = "SELECT access_count FROM daily_access_stats WHERE ip_address = ? AND access_date = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, today);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("access_count");
            }
        }
        return 0;
    }

    /**
     * 查询时间窗口内的访问次数
     */
    public int getAccessCountInTimeWindow(String ipAddress, LocalDateTime windowStart) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM access_log WHERE ip_address = ? AND request_time >= ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, windowStart.format(DATETIME_FORMATTER));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }
        }
        return 0;
    }

    /**
     * 检查最近是否已经发送过告警
     */
    public boolean isAlertSentRecently(String ipAddress, String alertType, LocalDateTime since) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM alert_log WHERE ip_address = ? AND alert_type = ? AND alert_time >= ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, alertType);
            pstmt.setString(3, since.format(DATETIME_FORMATTER));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        return false;
    }

    /**
     * 记录告警日志
     */
    public void insertAlertLog(String ipAddress, String alertType, int accessCount, int threshold, String message) throws SQLException {
        String sql = "INSERT INTO alert_log (ip_address, alert_type, access_count, threshold, alert_message) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, alertType);
            pstmt.setInt(3, accessCount);
            pstmt.setInt(4, threshold);
            pstmt.setString(5, message);
            pstmt.executeUpdate();
        }
    }

    /**
     * 检查数据库连接是否可用
     */
    public boolean isConnectionAvailable() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
