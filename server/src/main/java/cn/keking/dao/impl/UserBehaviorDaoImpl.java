package cn.keking.dao.impl;

import cn.keking.dao.UserBehaviorDao;
import cn.keking.model.UserBehaviorLog;
import cn.keking.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用户行为日志DAO实现
 * 使用SQLite数据库存储
 *
 * @author keking
 * @since 2025-07-17
 */
public class UserBehaviorDaoImpl implements UserBehaviorDao {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorDaoImpl.class);
    private static final String DB_NAME = "user_behavior.db";
    private static final String TABLE_NAME = "user_behavior_log";

    private Connection connection;
    private ExecutorService executorService;

    public UserBehaviorDaoImpl() {
        try {
            // 加载SQLite驱动
            Class.forName("org.sqlite.JDBC");
            
            // 数据库文件存储在项目home目录
            String dbPath = ConfigUtils.getHomePath() + File.separator + DB_NAME;
            
            // 建立数据库连接
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            // 创建表（如果不存在）
            createTable();
            
            // 初始化异步执行器
            executorService = Executors.newFixedThreadPool(2);
            
            logger.info("用户行为DAO初始化成功，数据库路径: {}", dbPath);
        } catch (ClassNotFoundException | SQLException e) {
            logger.error("用户行为DAO初始化失败", e);
            throw new RuntimeException("用户行为DAO初始化失败", e);
        }
    }

    /**
     * 创建用户行为日志表
     */
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n" +
                     "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                     "    ip_address TEXT NOT NULL,\n" +
                     "    file_name TEXT,\n" +
                     "    request_time TIMESTAMP NOT NULL,\n" +
                     "    request_url TEXT\n" +
                     ")";
        
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            logger.error("创建用户行为日志表失败", e);
        }
    }

    @Override
    public void save(UserBehaviorLog log) {
        // 异步插入日志，避免影响请求响应时间
        executorService.submit(() -> {
            String sql = "INSERT INTO " + TABLE_NAME + " (ip_address, file_name, request_time, request_url) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, log.getIpAddress());
                pstmt.setString(2, log.getFileName());
                pstmt.setTimestamp(3, Timestamp.from(log.getRequestTime().atZone(ZoneId.systemDefault()).toInstant()));
                pstmt.setString(4, log.getRequestUrl());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.error("保存用户行为日志失败", e);
            }
        });
    }

    @Override
    public void batchSave(List<UserBehaviorLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        
        // 异步批量插入
        executorService.submit(() -> {
            String sql = "INSERT INTO " + TABLE_NAME + " (ip_address, file_name, request_time, request_url) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                connection.setAutoCommit(false);
                
                for (UserBehaviorLog log : logs) {
                    pstmt.setString(1, log.getIpAddress());
                    pstmt.setString(2, log.getFileName());
                    pstmt.setTimestamp(3, Timestamp.from(log.getRequestTime().atZone(ZoneId.systemDefault()).toInstant()));
                    pstmt.setString(4, log.getRequestUrl());
                    pstmt.addBatch();
                }
                
                pstmt.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                logger.error("批量保存用户行为日志失败", e);
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    logger.error("回滚事务失败", ex);
                }
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                    logger.error("设置自动提交失败", ex);
                }
            }
        });
    }

    @Override
    public int getRequestCountInPeriod(String ipAddress, int minutes) {
        // 已使用Redis进行统计，此方法保留但返回0
        return 0;
    }

    @Override
    public int getRequestCountToday(String ipAddress) {
        // 已使用Redis进行统计，此方法保留但返回0
        return 0;
    }

    @Override
    public List<UserBehaviorLog> getLogsByIp(String ipAddress, int limit) {
        List<UserBehaviorLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE ip_address = ? ORDER BY request_time DESC LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ipAddress);
            pstmt.setInt(2, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UserBehaviorLog log = new UserBehaviorLog();
                    log.setId(rs.getLong("id"));
                    log.setIpAddress(rs.getString("ip_address"));
                    log.setFileName(rs.getString("file_name"));
                    log.setRequestTime(rs.getTimestamp("request_time").toLocalDateTime());
                    log.setRequestUrl(rs.getString("request_url"));
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            logger.error("获取用户行为日志失败", e);
        }
        
        return logs;
    }

    /**
     * 关闭资源
     */
    public void close() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("关闭数据库连接失败", e);
            }
        }
    }
}
