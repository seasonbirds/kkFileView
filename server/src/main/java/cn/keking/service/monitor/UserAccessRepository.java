package cn.keking.service.monitor;

import cn.keking.model.UserAccessLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * 用户访问日志数据访问层
 * 提供访问日志的增删查操作
 */
@Repository
public class UserAccessRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserAccessRepository.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * 表名
     */
    private static final String TABLE_NAME = "user_access_log";

    public UserAccessRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 初始化数据库表
     */
    @PostConstruct
    public void init() {
        try {
            createTableIfNotExists();
            logger.info("User access log table initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize user access log table", e);
        }
    }

    /**
     * 创建访问日志表（如果不存在）
     */
    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "ip_address VARCHAR(50) NOT NULL," +
                "file_name VARCHAR(255)," +
                "request_time TIMESTAMP NOT NULL," +
                "INDEX idx_ip_address (ip_address)," +
                "INDEX idx_request_time (request_time)" +
                ")";
        jdbcTemplate.execute(sql);
    }

    /**
     * 插入访问日志
     *
     * @param log 访问日志对象
     */
    public void insert(UserAccessLog log) {
        String sql = "INSERT INTO " + TABLE_NAME + " (ip_address, file_name, request_time) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, log.getIpAddress(), log.getFileName(), log.getRequestTime());
    }

    /**
     * 查询指定IP在时间窗口内的访问次数
     *
     * @param ipAddress    IP地址
     * @param timeWindow   时间窗口（分钟）
     * @return 访问次数
     */
    public int countByIpAndTimeWindow(String ipAddress, int timeWindow) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ip_address = ? AND request_time >= ?";
        long startTime = System.currentTimeMillis() - (long) timeWindow * 60 * 1000;
        return jdbcTemplate.queryForObject(sql, Integer.class, ipAddress, new Date(startTime));
    }

    /**
     * 查询指定IP当日的访问次数
     *
     * @param ipAddress IP地址
     * @return 访问次数
     */
    public int countByIpAndToday(String ipAddress) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE ip_address = ? AND DATE(request_time) = CURDATE()";
        return jdbcTemplate.queryForObject(sql, Integer.class, ipAddress);
    }

    /**
     * 查询所有访问日志
     *
     * @param limit 限制返回数量
     * @return 访问日志列表
     */
    public List<UserAccessLog> findAll(int limit) {
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY request_time DESC LIMIT ?";
        return jdbcTemplate.query(sql, new UserAccessLogRowMapper(), limit);
    }

    /**
     * UserAccessLog的RowMapper实现
     */
    private static class UserAccessLogRowMapper implements RowMapper<UserAccessLog> {
        @Override
        public UserAccessLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserAccessLog log = new UserAccessLog();
            log.setId(rs.getLong("id"));
            log.setIpAddress(rs.getString("ip_address"));
            log.setFileName(rs.getString("file_name"));
            log.setRequestTime(rs.getTimestamp("request_time"));
            return log;
        }
    }
}
