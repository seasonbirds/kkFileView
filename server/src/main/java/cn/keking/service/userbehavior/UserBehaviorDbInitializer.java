package cn.keking.service.userbehavior;

import cn.keking.config.ConfigConstants;
import cn.keking.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class UserBehaviorDbInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorDbInitializer.class);

    private static final String CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS user_behavior (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ip_address TEXT NOT NULL,
            file_name TEXT,
            request_time INTEGER NOT NULL,
            request_date TEXT NOT NULL
        )
        """;

    private static final String CREATE_INDEX_SQL = """
        CREATE INDEX IF NOT EXISTS idx_ip_time ON user_behavior(ip_address, request_time)
        """;

    private static final String CREATE_DATE_INDEX_SQL = """
        CREATE INDEX IF NOT EXISTS idx_ip_date ON user_behavior(ip_address, request_date)
        """;

    @Override
    public void run(String... args) {
        if (!ConfigConstants.isUserBehaviorMonitorEnabled()) {
            logger.info("用户行为监控未启用，跳过数据库初始化");
            return;
        }

        String dbPath = getDbPath();
        String jdbcUrl = "jdbc:sqlite:" + dbPath;

        logger.info("初始化用户行为监控数据库: {}", dbPath);

        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute(CREATE_TABLE_SQL);
            stmt.execute(CREATE_INDEX_SQL);
            stmt.execute(CREATE_DATE_INDEX_SQL);

            logger.info("用户行为监控数据库初始化完成");

        } catch (SQLException e) {
            logger.error("用户行为监控数据库初始化失败: {}", e.getMessage());
        }
    }

    public static String getDbPath() {
        String dbPath = ConfigConstants.getUserBehaviorDbPath();
        if (dbPath == null || "default".equalsIgnoreCase(dbPath)) {
            dbPath = ConfigUtils.getHomePath() + File.separator + "db" + File.separator;
        }

        File dbDir = new File(dbPath);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        return dbPath + "user_behavior.db";
    }
}
