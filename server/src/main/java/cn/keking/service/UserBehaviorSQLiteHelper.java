package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.model.UserBehaviorLog;
import cn.keking.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SQLite数据库助手类，用于用户行为记录的存储和查询
 *
 * @author keking
 * @since 2025-07-17
 */
public class UserBehaviorSQLiteHelper {
    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorSQLiteHelper.class);
    private static final String DB_NAME = "user_behavior.db";
    private static final String TABLE_NAME = "user_behavior_log";
    private static UserBehaviorSQLiteHelper instance;
    private Connection connection;
    private ExecutorService executorService;

    private UserBehaviorSQLiteHelper() {
        try {
            String dbPath = ConfigUtils.getHomePath() + File.separator + DB_NAME;
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTable();
            executorService = Executors.newFixedThreadPool(2);
            logger.info("SQLite数据库初始化成功，路径: {}", dbPath);
        } catch (Exception e) {
            logger.error("SQLite数据库初始化失败", e);
        }
    }

    public static synchronized UserBehaviorSQLiteHelper getInstance() {
        if (instance == null) {
            instance = new UserBehaviorSQLiteHelper();
        }
        return instance;
    }

    private void createTable() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ip_address TEXT NOT NULL, " +
                "file_name TEXT, " +
                "request_time TEXT NOT NULL, " +
                "request_url TEXT, " +
                "create_time TEXT DEFAULT (datetime('now', 'localtime')))";

        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
            logger.info("用户行为日志表创建成功");
        } catch (SQLException e) {
            logger.error("创建用户行为日志表失败", e);
        }
    }

    /**
     * 异步插入用户行为日志
     */
    public void insertLogAsync(UserBehaviorLog log) {
        if (executorService == null) {
            logger.warn("ExecutorService未初始化，无法异步插入日志");
            return;
        }
        executorService.submit(() -> insertLog(log));
    }

    /**
     * 同步插入用户行为日志
     */
    public void insertLog(UserBehaviorLog log) {
        String insertSQL = "INSERT INTO " + TABLE_NAME + " (ip_address, file_name, request_time, request_url) VALUES (?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertSQL)) {
            preparedStatement.setString(1, log.getIpAddress());
            preparedStatement.setString(2, log.getFileName());
            preparedStatement.setString(3, log.getRequestTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            preparedStatement.setString(4, log.getRequestUrl());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("插入用户行为日志失败", e);
        }
    }

    /**
     * 获取指定IP在指定时间段内的请求次数
     *
     * @param ipAddress IP地址
     * @param minutes   时间范围（分钟）
     * @return 请求次数
     */
    public int getRequestCountInPeriod(String ipAddress, int minutes) {
        String querySQL = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ip_address = ? AND request_time >= datetime('now', '-' || ? || ' minutes')";

        try (PreparedStatement preparedStatement = connection.prepareStatement(querySQL)) {
            preparedStatement.setString(1, ipAddress);
            preparedStatement.setInt(2, minutes);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("查询请求次数失败", e);
        }
        return 0;
    }

    /**
     * 获取指定IP当天的请求次数
     *
     * @param ipAddress IP地址
     * @return 请求次数
     */
    public int getRequestCountToday(String ipAddress) {
        String querySQL = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ip_address = ? AND date(request_time) = date('now', 'localtime')";

        try (PreparedStatement preparedStatement = connection.prepareStatement(querySQL)) {
            preparedStatement.setString(1, ipAddress);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("查询当天请求次数失败", e);
        }
        return 0;
    }

    /**
     * 关闭数据库连接和线程池
     */
    public void close() {
        try {
            if (executorService != null) {
                executorService.shutdown();
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            }
            if (connection != null) {
                connection.close();
            }
            logger.info("SQLite数据库连接已关闭");
        } catch (Exception e) {
            logger.error("关闭SQLite数据库连接失败", e);
        }
    }
}
