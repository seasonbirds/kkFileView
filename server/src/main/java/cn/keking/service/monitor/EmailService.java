package cn.keking.service.monitor;

import cn.keking.config.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Properties;

/**
 * 邮件服务类，封装邮件发送逻辑
 */
@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private JavaMailSenderImpl mailSender;

    /**
     * 初始化邮件发送器
     */
    @PostConstruct
    public void init() {
        if (!ConfigConstants.isBehaviorMonitorEnabled()) {
            return;
        }
        initMailSender();
        logger.info("Email service initialized");
    }

    /**
     * 配置邮件发送器参数
     */
    private void initMailSender() {
        mailSender = new JavaMailSenderImpl();
        mailSender.setHost(ConfigConstants.getBehaviorMonitorEmailHost());
        mailSender.setPort(ConfigConstants.getBehaviorMonitorEmailPort());
        mailSender.setUsername(ConfigConstants.getBehaviorMonitorEmailUsername());
        mailSender.setPassword(ConfigConstants.getBehaviorMonitorEmailPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
    }

    /**
     * 异步发送告警邮件
     *
     * @param ipAddress     IP地址
     * @param requestCount  请求次数
     * @param timeWindow    时间窗口（分钟）
     */
    @Async("monitorTaskExecutor")
    public void sendAlertEmail(String ipAddress, int requestCount, int timeWindow) {
        if (!ConfigConstants.isBehaviorMonitorEnabled() || mailSender == null) {
            return;
        }

        String to = ConfigConstants.getBehaviorMonitorEmailTo();
        if (to == null || to.isEmpty()) {
            logger.warn("Email recipient is not configured, skip sending alert email");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(ConfigConstants.getBehaviorMonitorEmailFrom());
            message.setTo(to.split(","));
            message.setSubject(ConfigConstants.getBehaviorMonitorEmailSubject());
            message.setText(String.format(ConfigConstants.getBehaviorMonitorEmailContent(),
                    ipAddress, timeWindow, requestCount));

            mailSender.send(message);
            logger.info("Alert email sent for IP: {}", ipAddress);
        } catch (Exception e) {
            logger.error("Failed to send alert email", e);
        }
    }
}
