package cn.keking.behavior.dao;

import cn.keking.behavior.BehaviorDatabaseInitializer;
import cn.keking.behavior.model.RequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * 请求日志数据访问层
 * 负责与SQLite数据库的交互
 */
@Repository
public class RequestLogDao {

    private static final Logger logger = LoggerFactory.getLogger(RequestLogDao.class);

    private static final String INSERT_SQL = "INSERT INTO request_log (ip_address, file_name, request_time) VALUES (?, ?, ?)";

    /**
     * 保存请求日志到数据库
     * @param requestLog 请求日志对象
     * @return 是否保存成功
     */
    public boolean save(RequestLog requestLog) {
        try (Connection connection = BehaviorDatabaseInitializer.getConnection();
             PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
            ps.setString(1, requestLog.getIpAddress());
            ps.setString(2, requestLog.getFileName());
            ps.setString(3, requestLog.getRequestTime().toString());
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            logger.error("Failed to save request log for IP: {}", requestLog.getIpAddress(), e);
            return false;
        }
    }

    /**
     * 批量保存请求日志
     * @param requestLogs 请求日志列表
     * @return 成功保存的数量
     */
    public int batchSave(Iterable<RequestLog> requestLogs) {
        int successCount = 0;
        try (Connection connection = BehaviorDatabaseInitializer.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
                for (RequestLog log : requestLogs) {
                    ps.setString(1, log.getIpAddress());
                    ps.setString(2, log.getFileName());
                    ps.setString(3, log.getRequestTime().toString());
                    ps.addBatch();
                    successCount++;
                }
                ps.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                logger.error("Failed to batch save request logs", e);
                return 0;
            }
        } catch (SQLException e) {
            logger.error("Failed to get connection for batch save", e);
            return 0;
        }
        return successCount;
    }
}
