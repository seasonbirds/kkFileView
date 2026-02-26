package cn.keking.behavior;

import cn.keking.behavior.config.BehaviorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 行为监控数据库初始化器
 * 负责初始化SQLite数据库和创建必要的表结构
 */
@Component
public class BehaviorDatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(BehaviorDatabaseInitializer.class);

    /**
     * 数据库文件路径，默认为用户主目录下的kkFileView目录
     */
    @Value("${behavior.db.path:${user.home}/kkFileView/behavior.db}")
    private String dbPath;

    /**
     * 创建请求日志表的SQL语句
     */
    private static final String CREATE_REQUEST_LOG_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS request_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ip_address VARCHAR(50) NOT NULL,
                file_name VARCHAR(500),
                request_time DATETIME NOT NULL
            )
            """;

    /**
     * 创建IP地址索引的SQL语句
     */
    private static final String CREATE_IP_INDEX_SQL = """
            CREATE INDEX IF NOT EXISTS idx_ip_address ON request_log(ip_address)
            """;

    /**
     * 创建请求时间索引的SQL语句
     */
    private static final String CREATE_TIME_INDEX_SQL = """
            CREATE INDEX IF NOT EXISTS idx_request_time ON request_log(request_time)
            """;

    /**
     * 数据源URL，静态变量供其他类使用
     */
    private static volatile String dataSourceUrl;

    /**
     * 初始化数据库
     * 在Bean创建后自动执行，创建数据库文件和表结构
     */
    @PostConstruct
    public void init() {
        try {
            File dbFile = new File(dbPath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            dataSourceUrl = "jdbc:sqlite:" + dbPath;

            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute(CREATE_REQUEST_LOG_TABLE_SQL);
                statement.execute(CREATE_IP_INDEX_SQL);
                statement.execute(CREATE_TIME_INDEX_SQL);
                logger.info("Behavior tracking database initialized successfully at: {}", dbPath);
            }
        } catch (SQLException e) {
            logger.error("Failed to initialize behavior tracking database", e);
        }
    }

    /**
     * 获取数据库连接
     * @return 数据库连接
     * @throws SQLException 数据库异常
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dataSourceUrl);
    }

    /**
     * 获取数据源URL
     * @return 数据源URL
     */
    public static String getDataSourceUrl() {
        return dataSourceUrl;
    }
}
