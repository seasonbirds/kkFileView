package cn.keking.security.service;

import cn.keking.security.config.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class EmailAlertService {

    private static final Logger logger = LoggerFactory.getLogger(EmailAlertService.class);

    private final SecurityConfig securityConfig;

    public EmailAlertService(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }

    public void sendAlertEmail(String ipAddress, int requestCount, int periodMinutes) {
        if (!securityConfig.isEmailEnabled()) {
            logger.debug("Email alert is disabled, skipping email sending");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", securityConfig.getEmailHost());
            props.put("mail.smtp.port", String.valueOf(securityConfig.getEmailPort()));
            
            if (securityConfig.getEmailPort() == 465) {
                props.put("mail.smtp.ssl.enable", "true");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }
            
            props.put("mail.smtp.auth", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            securityConfig.getEmailUsername(),
                            securityConfig.getEmailPassword());
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(securityConfig.getEmailFrom()));
            message.setRecipients(Message.RecipientType.TO, 
                    InternetAddress.parse(securityConfig.getEmailTo()));
            message.setSubject("用户行为异常");
            
            String content = String.format(
                    "IP地址%s在过去%d分钟内访问了%d次系统，超出正常范围，请保持关注。",
                    ipAddress, periodMinutes, requestCount);
            message.setText(content);

            Transport.send(message);
            logger.info("Alert email sent successfully to: {}", securityConfig.getEmailTo());

        } catch (MessagingException e) {
            logger.error("Failed to send alert email", e);
        }
    }

    public void sendDailyLimitAlertEmail(String ipAddress, int dailyCount, int dailyLimit) {
        if (!securityConfig.isEmailEnabled()) {
            logger.debug("Email alert is disabled, skipping email sending");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", securityConfig.getEmailHost());
            props.put("mail.smtp.port", String.valueOf(securityConfig.getEmailPort()));
            
            if (securityConfig.getEmailPort() == 465) {
                props.put("mail.smtp.ssl.enable", "true");
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }
            
            props.put("mail.smtp.auth", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            securityConfig.getEmailUsername(),
                            securityConfig.getEmailPassword());
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(securityConfig.getEmailFrom()));
            message.setRecipients(Message.RecipientType.TO, 
                    InternetAddress.parse(securityConfig.getEmailTo()));
            message.setSubject("用户行为异常");
            
            String content = String.format(
                    "IP地址%s今日已访问%d次，达到每日限制%d次，该IP已被临时禁止访问。",
                    ipAddress, dailyCount, dailyLimit);
            message.setText(content);

            Transport.send(message);
            logger.info("Daily limit alert email sent successfully to: {}", securityConfig.getEmailTo());

        } catch (MessagingException e) {
            logger.error("Failed to send daily limit alert email", e);
        }
    }
}
