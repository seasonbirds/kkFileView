package cn.keking.audit.repository;

import cn.keking.audit.model.AlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {

    List<AlertRecord> findByIpOrderByAlertTimeDesc(String ip);

    @Query("SELECT a FROM AlertRecord a WHERE a.ip = :ip AND a.alertTime >= :startTime ORDER BY a.alertTime DESC")
    List<AlertRecord> findByIpAndAlertTimeAfter(@Param("ip") String ip, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT COUNT(a) > 0 FROM AlertRecord a WHERE a.ip = :ip AND a.alertTime >= :startTime")
    boolean existsByIpAndAlertTimeAfter(@Param("ip") String ip, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT a FROM AlertRecord a WHERE a.alertTime < :cutoffTime")
    List<AlertRecord> findByAlertTimeBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
}
