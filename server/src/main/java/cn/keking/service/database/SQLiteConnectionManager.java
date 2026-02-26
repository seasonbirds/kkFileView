package cn.keking.service.database;

import cn.keking.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteConnectionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLiteConnectionManager.class);
    private static final String DB_NAME = "user_behavior.db";
    private static volatile boolean initialized = false;

    public static String getDbPath() {
        String homePath = ConfigUtils.getHomePath();
        String separator = File.separator;
        return homePath + separator + DB_NAME;
    }

    public static Connection getConnection() throws SQLException {
        String dbPath = getDbPath();
        String url = "jdbc:sqlite:" + dbPath;
        return DriverManager.getConnection(url);
    }

    public static synchronized void initializeDatabase() {
        if (initialized) {
            return;
        }
        String createTableSql = "CREATE TABLE IF NOT EXISTS user_behavior (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ip_address TEXT NOT NULL," +
                "file_name TEXT," +
                "request_url TEXT," +
                "request_time TIMESTAMP DEFAULT (datetime('now', 'localtime'))" +
                ");" +
                "CREATE INDEX IF NOT EXISTS idx_ip_time ON user_behavior(ip_address, request_time);" +
                "CREATE INDEX IF NOT EXISTS idx_time ON user_behavior(request_time);";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableSql);
            initialized = true;
            LOGGER.info("SQLite数据库初始化成功，路径: {}", getDbPath());
        } catch (SQLException e) {
            LOGGER.error("SQLite数据库初始化失败", e);
        }
    }

    public static void closeQuietly(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    LOGGER.warn("关闭资源失败", e);
                }
            }
        }
    }
}
