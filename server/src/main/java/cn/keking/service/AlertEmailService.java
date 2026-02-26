package cn.keking.service;

import cn.keking.config.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlertEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertEmailService.class);
    private final ExecutorService emailExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AlertEmailSender");
        t.setDaemon(true);
        return t;
    });

    public void sendPeriodAlert(String ipAddress, int periodMinutes, int requestCount) {
        String template = ConfigConstants.getPeriodAlertTemplate();
        String content = String.format(template, ipAddress, periodMinutes, requestCount);
        sendAlertEmailAsync(content);
    }

    public void sendDailyAlert(String ipAddress, int dailyCount) {
        String template = ConfigConstants.getDailyAlertTemplate();
        String content = String.format(template, ipAddress, dailyCount);
        sendAlertEmailAsync(content);
    }

    private void sendAlertEmailAsync(String content) {
        emailExecutor.submit(() -> {
            try {
                doSendEmail(content);
            } catch (Exception e) {
                LOGGER.error("发送告警邮件失败", e);
            }
        });
    }

    private void doSendEmail(String content) {
        String host = ConfigConstants.getMailHost();
        int port = ConfigConstants.getMailPort();
        String username = ConfigConstants.getMailUsername();
        String password = ConfigConstants.getMailPassword();
        String from = ConfigConstants.getMailFrom();
        String to = ConfigConstants.getMailTo();

        if (host == null || host.isEmpty() ||
            username == null || username.isEmpty() ||
            to == null || to.isEmpty()) {
            LOGGER.debug("邮件配置不完整，跳过发送告警邮件");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", host);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(ConfigConstants.getMailSubject());
            message.setText(content);
            Transport.send(message);
            LOGGER.info("告警邮件发送成功");
        } catch (MessagingException e) {
            LOGGER.error("发送告警邮件失败", e);
        }
    }
}
