package cn.keking.audit.service;

import cn.keking.audit.config.AuditConfig;
import cn.keking.audit.model.AlertRecord;
import cn.keking.audit.repository.AlertRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Properties;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailAlertService {

    private static final Logger logger = LoggerFactory.getLogger(EmailAlertService.class);

    private final AlertRecordRepository alertRecordRepository;

    public EmailAlertService(AlertRecordRepository alertRecordRepository) {
        this.alertRecordRepository = alertRecordRepository;
    }

    @Async("emailTaskExecutor")
    public void sendAlertAsync(String ip, int accessCount, int timeWindowMinutes, String alertType) {
        if (!isEmailConfigured()) {
            logger.warn("Email not configured, skipping alert for IP: {}", ip);
            return;
        }

        try {
            String subject = "用户行为异常";
            String content = String.format(
                "IP地址%s在过去%d分钟内访问了%d次系统，超出正常范围，请保持关注。",
                ip, timeWindowMinutes, accessCount
            );

            sendEmail(subject, content);

            saveAlertRecord(ip, accessCount, timeWindowMinutes, content, alertType);

            logger.info("Alert email sent for IP: {}, count: {}", ip, accessCount);
        } catch (Exception e) {
            logger.error("Failed to send alert email for IP: {}", ip, e);
        }
    }

    private boolean isEmailConfigured() {
        String host = AuditConfig.getEmailHost();
        String adminEmail = AuditConfig.getAdminEmail();
        return host != null && !host.isEmpty() && 
               adminEmail != null && !adminEmail.isEmpty();
    }

    private void sendEmail(String subject, String content) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", AuditConfig.getEmailHost());
        props.put("mail.smtp.port", AuditConfig.getEmailPort());
        props.put("mail.smtp.auth", AuditConfig.isEmailAuthEnabled());
        props.put("mail.smtp.starttls.enable", AuditConfig.isEmailStartTlsEnabled());
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Authenticator auth = null;
        if (AuditConfig.isEmailAuthEnabled()) {
            final String username = AuditConfig.getEmailUsername();
            final String password = AuditConfig.getEmailPassword();
            auth = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
        }

        Session session = Session.getInstance(props, auth);

        MimeMessage message = new MimeMessage(session);
        String from = AuditConfig.getEmailFrom();
        if (from == null || from.isEmpty()) {
            from = AuditConfig.getEmailUsername();
        }
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, 
            InternetAddress.parse(AuditConfig.getAdminEmail()));
        message.setSubject(subject, "UTF-8");
        message.setText(content, "UTF-8");
        message.setSentDate(new java.util.Date());

        Transport.send(message);
    }

    private void saveAlertRecord(String ip, int accessCount, int timeWindowMinutes, 
                                   String alertMessage, String alertType) {
        try {
            AlertRecord record = new AlertRecord(
                ip,
                accessCount,
                timeWindowMinutes,
                LocalDateTime.now(),
                alertMessage,
                alertType
            );
            alertRecordRepository.save(record);
        } catch (Exception e) {
            logger.error("Failed to save alert record", e);
        }
    }
}
