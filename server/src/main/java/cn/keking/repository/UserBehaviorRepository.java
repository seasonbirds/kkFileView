package cn.keking.repository;

import cn.keking.model.UserBehavior;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户行为记录数据访问层
 */
@Repository
public class UserBehaviorRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 保存用户行为记录
     */
    public UserBehavior save(UserBehavior userBehavior) {
        if (userBehavior.getId() == null) {
            entityManager.persist(userBehavior);
            return userBehavior;
        } else {
            return entityManager.merge(userBehavior);
        }
    }

    /**
     * 查询指定IP在指定时间范围内的访问次数
     */
    public long countByIpAddressAndRequestTimeBetween(String ipAddress, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT COUNT(*) FROM user_behavior WHERE ip_address = :ipAddress AND request_time BETWEEN :startTime AND :endTime";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("ipAddress", ipAddress);
        query.setParameter("startTime", startTime);
        query.setParameter("endTime", endTime);
        return ((Number) query.getSingleResult()).longValue();
    }

    /**
     * 查询指定IP在指定日期内的访问次数
     */
    public long countByIpAddressAndDate(String ipAddress, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date.toLocalDate().atTime(23, 59, 59);
        return countByIpAddressAndRequestTimeBetween(ipAddress, startOfDay, endOfDay);
    }

    /**
     * 查询指定IP在指定时间范围内的所有访问记录
     */
    public List<UserBehavior> findByIpAddressAndRequestTimeBetween(String ipAddress, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT * FROM user_behavior WHERE ip_address = :ipAddress AND request_time BETWEEN :startTime AND :endTime ORDER BY request_time DESC";
        Query query = entityManager.createNativeQuery(sql, UserBehavior.class);
        query.setParameter("ipAddress", ipAddress);
        query.setParameter("startTime", startTime);
        query.setParameter("endTime", endTime);
        return query.getResultList();
    }

    /**
     * 查询指定IP在指定日期内的所有访问记录
     */
    public List<UserBehavior> findByIpAddressAndDate(String ipAddress, LocalDateTime date) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = date.toLocalDate().atTime(23, 59, 59);
        return findByIpAddressAndRequestTimeBetween(ipAddress, startOfDay, endOfDay);
    }

    /**
     * 删除指定时间之前的记录（用于清理旧数据）
     */
    public int deleteByRequestTimeBefore(LocalDateTime time) {
        String sql = "DELETE FROM user_behavior WHERE request_time < :time";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("time", time);
        return query.executeUpdate();
    }

    /**
     * 查询所有异常行为记录
     */
    public List<UserBehavior> findByIsAbnormalTrue() {
        String sql = "SELECT * FROM user_behavior WHERE is_abnormal = 1 ORDER BY request_time DESC";
        Query query = entityManager.createNativeQuery(sql, UserBehavior.class);
        return query.getResultList();
    }
}