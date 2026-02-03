package cn.keking.service;

import cn.keking.config.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * 邮件服务
 * 用于发送告警邮件
 *
 * @author kkFileView
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    /**
     * 发送告警邮件
     *
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendAlertEmail(String subject, String content) {
        if (!ConfigConstants.isUserBehaviorEmailEnabled()) {
            logger.debug("邮件告警功能未启用");
            return;
        }

        String smtpHost = ConfigConstants.getUserBehaviorEmailSmtpHost();
        int smtpPort = ConfigConstants.getUserBehaviorEmailSmtpPort();
        String username = ConfigConstants.getUserBehaviorEmailUsername();
        String password = ConfigConstants.getUserBehaviorEmailPassword();
        String from = ConfigConstants.getUserBehaviorEmailFrom();
        String to = ConfigConstants.getUserBehaviorEmailTo();

        if (smtpHost == null || smtpHost.isEmpty() ||
                username == null || username.isEmpty() ||
                password == null || password.isEmpty() ||
                to == null || to.isEmpty()) {
            logger.warn("邮件配置不完整，无法发送告警邮件");
            return;
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.timeout", "10000");
            props.put("mail.smtp.connectiontimeout", "10000");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from != null && !from.isEmpty() ? from : username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(content);

            Transport.send(message);
            logger.info("告警邮件发送成功 - 收件人: {}", to);

        } catch (Exception e) {
            logger.error("发送告警邮件失败", e);
        }
    }
}
