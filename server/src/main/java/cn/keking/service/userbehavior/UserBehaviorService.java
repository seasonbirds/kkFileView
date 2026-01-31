package cn.keking.service.userbehavior;

import cn.keking.config.ConfigConstants;
import cn.keking.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserBehaviorService {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorService.class);
    
    private static final String TABLE_NAME = "user_behavior";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_VALUE = "default";

    @PostConstruct
    public void init() {
        if (!ConfigConstants.isUserBehaviorMonitorEnabled()) {
            return;
        }
        createTableIfNotExists();
    }

    private Connection getConnection() throws SQLException {
        String dbPath = ConfigConstants.getUserBehaviorDbPath();
        if (DEFAULT_VALUE.equalsIgnoreCase(dbPath)) {
            dbPath = ConfigUtils.getHomePath() + File.separator + "data" + File.separator + "user_behavior.db";
        }
        File dbFile = new File(dbPath);
        if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
        }
        String url = "jdbc:sqlite:" + dbPath;
        return DriverManager.getConnection(url);
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ip_address TEXT NOT NULL," +
                "file_name TEXT," +
                "request_time INTEGER NOT NULL," +
                "request_date TEXT NOT NULL" +
                ")";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("User behavior table initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to create user behavior table", e);
        }
    }

    public void recordBehavior(String ipAddress, String fileName) {
        String sql = "INSERT INTO " + TABLE_NAME + " (ip_address, file_name, request_time, request_date) VALUES (?, ?, ?, ?)";
        long currentTime = System.currentTimeMillis();
        String currentDate = LocalDate.now().format(DATE_FORMATTER);
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, fileName);
            pstmt.setLong(3, currentTime);
            pstmt.setString(4, currentDate);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to record user behavior", e);
        }
    }

    public int countRequestsInPeriod(String ipAddress, int minutes) {
        long currentTime = System.currentTimeMillis();
        long periodStartTime = currentTime - (long) minutes * 60 * 1000;
        
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ip_address = ? AND request_time >= ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setLong(2, periodStartTime);
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

    public int countRequestsToday(String ipAddress) {
        String today = LocalDate.now().format(DATE_FORMATTER);
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ip_address = ? AND request_date = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setString(2, today);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to count requests today", e);
        }
        return 0;
    }

    public List<UserBehavior> getRecentBehaviors(String ipAddress, int limit) {
        List<UserBehavior> behaviors = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE ip_address = ? ORDER BY request_time DESC LIMIT ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UserBehavior behavior = new UserBehavior();
                    behavior.setId(rs.getLong("id"));
                    behavior.setIpAddress(rs.getString("ip_address"));
                    behavior.setFileName(rs.getString("file_name"));
                    behavior.setRequestTime(rs.getLong("request_time"));
                    behavior.setRequestDate(rs.getString("request_date"));
                    behaviors.add(behavior);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get recent behaviors", e);
        }
        return behaviors;
    }

}
