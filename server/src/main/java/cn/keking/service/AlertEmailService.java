package cn.keking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 告警邮件服务
 */
@Service
public class AlertEmailService {

    private static final Logger logger = LoggerFactory.getLogger(AlertEmailService.class);

    private final JavaMailSender mailSender;

    @Autowired
    public AlertEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 异步发送异常行为告警邮件
     * @param toEmail 收件人邮箱
     * @param ipAddress IP地址
     * @param periodMinutes 统计周期（分钟）
     * @param accessCount 访问次数
     */
    @Async("taskExecutor")
    public void sendAbnormalBehaviorAlert(String toEmail, String ipAddress, int periodMinutes, long accessCount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("用户行为异常");
            String content = String.format("IP地址%s在过去%d分钟内访问了%d次系统，超出正常范围，请保持关注。", ipAddress, periodMinutes, accessCount);
            message.setText(content);
            mailSender.send(message);
            logger.info("告警邮件已发送至: {}, IP: {}, 访问次数: {}", toEmail, ipAddress, accessCount);
        } catch (Exception e) {
            logger.error("发送告警邮件失败: IP={}, 错误={}", ipAddress, e.getMessage(), e);
        }
    }
}
