package cn.keking.audit.config;

import cn.keking.utils.ConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * SQLite数据源配置类
 * 用于配置用户行为审计功能的SQLite数据库连接
 */
@Configuration
public class SQLiteDataSourceConfig {

    /**
     * 创建SQLite数据源
     * 数据库文件存储在项目data目录下的audit.db
     *
     * @return DataSource数据源对象
     */
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");

        String dbPath = getDbPath();
        File dbDir = new File(dbPath).getParentFile();
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        dataSource.setUrl("jdbc:sqlite:" + dbPath);

        Map<String, String> connectionProperties = new HashMap<>();
        connectionProperties.put("foreign_keys", "true");
        connectionProperties.put("journal_mode", "WAL");
        connectionProperties.put("synchronous", "NORMAL");
        dataSource.setConnectionProperties(connectionProperties);

        return dataSource;
    }

    /**
     * 获取数据库文件路径
     *
     * @return 数据库文件完整路径
     */
    private String getDbPath() {
        String homePath = ConfigUtils.getHomePath();
        return homePath + File.separator + "data" + File.separator + "audit.db";
    }
}
