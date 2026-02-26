package cn.keking.service.database;

import cn.keking.model.UserBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserBehaviorDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserBehaviorDao.class);

    public void insert(UserBehavior behavior) {
        String sql = "INSERT INTO user_behavior(ip_address, file_name, request_url, request_time) VALUES(?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = SQLiteConnectionManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, behavior.getIpAddress());
            pstmt.setString(2, behavior.getFileName());
            pstmt.setString(3, behavior.getRequestUrl());
            pstmt.setTimestamp(4, Timestamp.valueOf(behavior.getRequestTime()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("保存用户行为记录失败", e);
        } finally {
            SQLiteConnectionManager.closeQuietly(pstmt, conn);
        }
    }

    public void batchInsert(List<UserBehavior> behaviors) {
        if (behaviors == null || behaviors.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO user_behavior(ip_address, file_name, request_url, request_time) VALUES(?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = SQLiteConnectionManager.getConnection();
            conn.setAutoCommit(false);
            pstmt = conn.prepareStatement(sql);
            for (UserBehavior behavior : behaviors) {
                pstmt.setString(1, behavior.getIpAddress());
                pstmt.setString(2, behavior.getFileName());
                pstmt.setString(3, behavior.getRequestUrl());
                pstmt.setTimestamp(4, Timestamp.valueOf(behavior.getRequestTime()));
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            LOGGER.error("批量保存用户行为记录失败", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.error("回滚失败", ex);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    LOGGER.warn("恢复自动提交失败", e);
                }
            }
            SQLiteConnectionManager.closeQuietly(pstmt, conn);
        }
    }

    public int countByIpAndTimeRange(String ipAddress, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT COUNT(*) FROM user_behavior WHERE ip_address = ? AND request_time >= ? AND request_time < ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = SQLiteConnectionManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, ipAddress);
            pstmt.setTimestamp(2, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(3, Timestamp.valueOf(endTime));
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error("统计用户请求次数失败", e);
        } finally {
            SQLiteConnectionManager.closeQuietly(rs, pstmt, conn);
        }
        return 0;
    }

    public int countByIpAndToday(String ipAddress, LocalDateTime todayStart, LocalDateTime tomorrowStart) {
        return countByIpAndTimeRange(ipAddress, todayStart, tomorrowStart);
    }

    public List<UserBehavior> findByIpAndTimeRange(String ipAddress, LocalDateTime startTime, LocalDateTime endTime, int limit) {
        String sql = "SELECT id, ip_address, file_name, request_url, request_time FROM user_behavior WHERE ip_address = ? AND request_time >= ? AND request_time < ? ORDER BY request_time DESC LIMIT ?";
        List<UserBehavior> result = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = SQLiteConnectionManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, ipAddress);
            pstmt.setTimestamp(2, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(3, Timestamp.valueOf(endTime));
            pstmt.setInt(4, limit);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                UserBehavior behavior = new UserBehavior();
                behavior.setId(rs.getLong("id"));
                behavior.setIpAddress(rs.getString("ip_address"));
                behavior.setFileName(rs.getString("file_name"));
                behavior.setRequestUrl(rs.getString("request_url"));
                behavior.setRequestTime(rs.getTimestamp("request_time").toLocalDateTime());
                result.add(behavior);
            }
        } catch (SQLException e) {
            LOGGER.error("查询用户行为记录失败", e);
        } finally {
            SQLiteConnectionManager.closeQuietly(rs, pstmt, conn);
        }
        return result;
    }

    public void cleanOldRecords(LocalDateTime beforeTime) {
        String sql = "DELETE FROM user_behavior WHERE request_time < ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = SQLiteConnectionManager.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setTimestamp(1, Timestamp.valueOf(beforeTime));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("清理过期用户行为记录失败", e);
        } finally {
            SQLiteConnectionManager.closeQuietly(pstmt, conn);
        }
    }
}
