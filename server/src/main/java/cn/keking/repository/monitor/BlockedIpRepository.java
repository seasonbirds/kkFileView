package cn.keking.repository.monitor;

import cn.keking.model.monitor.BlockedIp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 封禁IP数据访问接口
 * 用于存储和查询IP封禁记录
 */
@Repository
public interface BlockedIpRepository extends JpaRepository<BlockedIp, Long> {

    /**
     * 根据IP地址查找封禁记录
     */
    Optional<BlockedIp> findByIpAddress(String ipAddress);

    /**
     * 查询所有当前有效的封禁记录
     * 用于启动时加载到内存缓存
     */
    @Query("SELECT b FROM BlockedIp b WHERE b.blockEndTime > :now")
    List<BlockedIp> findAllActiveBlocks(@Param("now") LocalDateTime now);
}
