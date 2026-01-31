package cn.keking.service;

import cn.keking.config.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 邮件告警服务类
 */
@Service
public class AlertEmailService {

    private static final Logger logger = LoggerFactory.getLogger(AlertEmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    /**
     * 异步发送用户行为异常告警邮件
     */
    @Async("mailExecutor")
    public CompletableFuture<Void> sendAbnormalBehaviorAlert(String ipAddress, int timeWindowMinutes, int accessCount) {
        if (!ConfigConstants.isAlertEmailEnabled()) {
            logger.debug("邮件告警功能未启用，跳过发送告警邮件");
            return CompletableFuture.completedFuture(null);
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(ConfigConstants.getMailFrom());
            message.setTo(ConfigConstants.getAlertEmailTo());
            message.setSubject("用户行为异常");
            
            String content = String.format("IP地址%s在过去%d分钟内访问了%d次系统，超出正常范围，请保持关注。", 
                    ipAddress, timeWindowMinutes, accessCount);
            message.setText(content);
            
            mailSender.send(message);
            logger.info("已发送用户行为异常告警邮件: {}", content);
        } catch (Exception e) {
            logger.error("发送用户行为异常告警邮件失败", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步发送每日访问超限告警邮件
     */
    @Async("mailExecutor")
    public CompletableFuture<Void> sendDailyAccessExceededAlert(String ipAddress, int accessCount) {
        if (!ConfigConstants.isAlertEmailEnabled()) {
            logger.debug("邮件告警功能未启用，跳过发送告警邮件");
            return CompletableFuture.completedFuture(null);
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(ConfigConstants.getMailFrom());
            message.setTo(ConfigConstants.getAlertEmailTo());
            message.setSubject("用户行为异常");
            
            String content = String.format("IP地址%s在今天已访问系统%d次，超出每日访问限制，已被禁止访问。", 
                    ipAddress, accessCount);
            message.setText(content);
            
            mailSender.send(message);
            logger.info("已发送每日访问超限告警邮件: {}", content);
        } catch (Exception e) {
            logger.error("发送每日访问超限告警邮件失败", e);
        }
        return CompletableFuture.completedFuture(null);
    }
}