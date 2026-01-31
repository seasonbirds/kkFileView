package cn.keking.service.userbehavior;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${user.behavior.monitor.enabled:false}'.equals('true')")
public class UserBehaviorScheduler {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorScheduler.class);

    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanDailyBlockedIps() {
        logger.info("开始清理当日IP限制列表");
        UserBehaviorAccessManager.clearDailyData();
        logger.info("清理当日IP限制列表完成");
    }
}