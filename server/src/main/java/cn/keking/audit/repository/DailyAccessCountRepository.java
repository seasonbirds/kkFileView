package cn.keking.audit.repository;

import cn.keking.audit.model.DailyAccessCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyAccessCountRepository extends JpaRepository<DailyAccessCount, Long> {

    Optional<DailyAccessCount> findByIpAndAccessDate(String ip, LocalDate accessDate);

    @Query("SELECT COALESCE(SUM(d.accessCount), 0) FROM DailyAccessCount d WHERE d.ip = :ip AND d.accessDate = :accessDate")
    int sumAccessCountByIpAndAccessDate(@Param("ip") String ip, @Param("accessDate") LocalDate accessDate);

    @Query("SELECT COALESCE(d.accessCount, 0) FROM DailyAccessCount d WHERE d.ip = :ip AND d.accessDate = :accessDate")
    Optional<Integer> findAccessCountByIpAndAccessDate(@Param("ip") String ip, @Param("accessDate") LocalDate accessDate);
}
