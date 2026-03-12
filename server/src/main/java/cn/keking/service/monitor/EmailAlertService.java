package cn.keking.service.monitor;

import cn.keking.config.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailAlertService {

    private static final Logger logger = LoggerFactory.getLogger(EmailAlertService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender javaMailSender;

    public EmailAlertService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * 异步发送IP封禁告警邮件
     * 使用专用邮件线程池
     * @param ipAddress 被封禁的IP
     * @param reason 封禁原因
     * @param blockEndTime 封禁结束时间
     */
    @Async("emailTaskExecutor")
    public void sendBlockAlert(String ipAddress, String reason, LocalDateTime blockEndTime) {
        if (!ConfigConstants.getMonitorAlertEmailEnabled()) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(ConfigConstants.getMonitorAlertEmailTo().split(","));
            message.setSubject(ConfigConstants.getMonitorAlertEmailSubject());
            message.setText(buildBlockAlertContent(ipAddress, reason, blockEndTime));
            javaMailSender.send(message);
            logger.info("发送告警邮件到: {}", ConfigConstants.getMonitorAlertEmailTo());
        } catch (Exception e) {
            logger.error("发送告警邮件失败", e);
        }
    }

    private String buildBlockAlertContent(String ipAddress, String reason, LocalDateTime blockEndTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("kkFileView 异常访问检测告警\n");
        sb.append("================================\n\n");
        sb.append("IP地址: ").append(ipAddress).append("\n");
        sb.append("封禁原因: ").append(reason).append("\n");
        sb.append("封禁开始时间: ").append(LocalDateTime.now().format(FORMATTER)).append("\n");
        sb.append("预计解封时间: ").append(blockEndTime.format(FORMATTER)).append("\n\n");
        sb.append("请检查该IP的访问行为是否正常。\n");
        sb.append("\n系统自动发送，请勿回复。");
        return sb.toString();
    }
}
