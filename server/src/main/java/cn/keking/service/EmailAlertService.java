package cn.keking.service;

import cn.keking.config.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * 邮件告警服务
 *
 * @author keking
 * @since 2025-07-17
 */
public class EmailAlertService {
    private static final Logger logger = LoggerFactory.getLogger(EmailAlertService.class);

    /**
     * 发送告警邮件
     *
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendAlert(String subject, String content) {
        // 检查邮件配置是否完整
        if (!isEmailConfigured()) {
            logger.warn("邮件告警配置不完整，无法发送告警邮件");
            return;
        }

        String smtpHost = ConfigConstants.getUserBehaviorAnalysisSmtpHost();
        int smtpPort = ConfigConstants.getUserBehaviorAnalysisSmtpPort();
        String username = ConfigConstants.getUserBehaviorAnalysisSmtpUsername();
        String password = ConfigConstants.getUserBehaviorAnalysisSmtpPassword();
        String from = ConfigConstants.getUserBehaviorAnalysisSmtpFrom();
        String to = ConfigConstants.getUserBehaviorAnalysisSmtpTo();

        // 设置邮件属性
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // TLS

        // 创建会话
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            // 创建邮件消息
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(content);

            // 发送邮件
            Transport.send(message);
            logger.info("告警邮件已发送至: {}", to);

        } catch (MessagingException e) {
            logger.error("发送告警邮件失败", e);
        }
    }

    /**
     * 检查邮件配置是否完整
     *
     * @return 配置完整返回true，否则返回false
     */
    private boolean isEmailConfigured() {
        return ConfigConstants.getUserBehaviorAnalysisSmtpHost() != null
                && !ConfigConstants.getUserBehaviorAnalysisSmtpHost().isEmpty()
                && ConfigConstants.getUserBehaviorAnalysisSmtpPort() > 0
                && ConfigConstants.getUserBehaviorAnalysisSmtpUsername() != null
                && !ConfigConstants.getUserBehaviorAnalysisSmtpUsername().isEmpty()
                && ConfigConstants.getUserBehaviorAnalysisSmtpPassword() != null
                && !ConfigConstants.getUserBehaviorAnalysisSmtpPassword().isEmpty()
                && ConfigConstants.getUserBehaviorAnalysisSmtpFrom() != null
                && !ConfigConstants.getUserBehaviorAnalysisSmtpFrom().isEmpty()
                && ConfigConstants.getUserBehaviorAnalysisSmtpTo() != null
                && !ConfigConstants.getUserBehaviorAnalysisSmtpTo().isEmpty();
    }
}
