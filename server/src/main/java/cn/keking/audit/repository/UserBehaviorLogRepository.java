package cn.keking.audit.repository;

import cn.keking.audit.model.UserBehaviorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserBehaviorLogRepository extends JpaRepository<UserBehaviorLog, Long> {

    @Query("SELECT COUNT(u) FROM UserBehaviorLog u WHERE u.ip = :ip AND u.requestTime >= :startTime")
    long countByIpAndRequestTimeAfter(@Param("ip") String ip, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT COUNT(u) FROM UserBehaviorLog u WHERE u.ip = :ip AND u.requestTime BETWEEN :startTime AND :endTime")
    long countByIpAndRequestTimeBetween(@Param("ip") String ip, 
                                          @Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);

    List<UserBehaviorLog> findByIpOrderByRequestTimeDesc(String ip);

    @Query("SELECT u FROM UserBehaviorLog u WHERE u.requestTime < :cutoffTime")
    List<UserBehaviorLog> findByRequestTimeBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

    void deleteByRequestTimeBefore(LocalDateTime cutoffTime);
}
