package cn.keking.repository.monitor;

import cn.keking.model.monitor.UserBehavior;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户行为数据访问接口
 * 只保留必要的保存方法用于记录用户访问日志
 */
@Repository
public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {
}
