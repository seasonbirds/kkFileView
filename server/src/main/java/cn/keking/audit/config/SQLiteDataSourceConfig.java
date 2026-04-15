package cn.keking.audit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class SQLiteDataSourceConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        String dbPath = AuditConfig.getDbPath();
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
}
