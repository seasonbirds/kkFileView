package cn.keking.behavior.service;

import cn.keking.behavior.config.BehaviorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * 告警邮件服务类
 * 负责发送异常行为告警邮件
 */
@Service
public class AlertMailService {

    private static final Logger logger = LoggerFactory.getLogger(AlertMailService.class);

    private volatile JavaMailSender mailSender;

    /**
     * 获取或初始化邮件发送器
     * 使用懒加载和双重检查锁定确保线程安全
     */
    private JavaMailSender getMailSender() {
        if (mailSender == null) {
            synchronized (this) {
                if (mailSender == null) {
                    JavaMailSenderImpl sender = new JavaMailSenderImpl();
                    sender.setHost(BehaviorConfig.getMailHost());
                    sender.setPort(BehaviorConfig.getMailPort());
                    sender.setUsername(BehaviorConfig.getMailUsername());
                    sender.setPassword(BehaviorConfig.getMailPassword());
                    sender.setDefaultEncoding("UTF-8");

                    Properties props = sender.getJavaMailProperties();
                    props.put("mail.transport.protocol", "smtp");
                    props.put("mail.smtp.auth", "true");
                    if (BehaviorConfig.isMailSslEnabled()) {
                        props.put("mail.smtp.ssl.enable", "true");
                        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    } else {
                        props.put("mail.smtp.starttls.enable", "true");
                    }
                    props.put("mail.smtp.connectiontimeout", "5000");
                    props.put("mail.smtp.timeout", "5000");

                    mailSender = sender;
                }
            }
        }
        return mailSender;
    }

    /**
     * 异步发送周期内访问频率超限告警邮件
     * @param ipAddress IP地址
     * @param requestCount 访问次数
     * @param periodMinutes 统计周期（分钟）
     */
    @Async("behaviorTaskExecutor")
    public void sendPeriodAlertAsync(String ipAddress, int requestCount, int periodMinutes) {
        if (!isMailConfigured()) {
            logger.warn("Mail not configured, skip sending period alert for IP: {}", ipAddress);
            return;
        }

        try {
            String subject = BehaviorConfig.getMailPeriodAlertSubject();
            String content = BehaviorConfig.getMailPeriodAlertContent()
                    .replace("{ip}", ipAddress)
                    .replace("{minutes}", String.valueOf(periodMinutes))
                    .replace("{count}", String.valueOf(requestCount));

            sendMail(subject, content);
            logger.info("Period alert email sent for IP: {}", ipAddress);
        } catch (Exception e) {
            logger.error("Failed to send period alert email for IP: {}", ipAddress, e);
        }
    }

    /**
     * 异步发送每日访问量超限告警邮件
     * @param ipAddress IP地址
     * @param requestCount 访问次数
     */
    @Async("behaviorTaskExecutor")
    public void sendDailyAlertAsync(String ipAddress, int requestCount) {
        if (!isMailConfigured()) {
            logger.warn("Mail not configured, skip sending daily alert for IP: {}", ipAddress);
            return;
        }

        try {
            String subject = BehaviorConfig.getMailDailyAlertSubject();
            String content = BehaviorConfig.getMailDailyAlertContent()
                    .replace("{ip}", ipAddress)
                    .replace("{count}", String.valueOf(requestCount));

            sendMail(subject, content);
            logger.info("Daily alert email sent for IP: {}", ipAddress);
        } catch (Exception e) {
            logger.error("Failed to send daily alert email for IP: {}", ipAddress, e);
        }
    }

    /**
     * 发送邮件
     * @param subject 邮件标题
     * @param content 邮件内容
     */
    private void sendMail(String subject, String content) {
        try {
            JavaMailSender sender = getMailSender();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(BehaviorConfig.getMailFrom());
            helper.setTo(BehaviorConfig.getMailTo());
            helper.setSubject(subject);
            helper.setText(content, false);

            sender.send(message);
            logger.info("Alert email sent successfully: {}", subject);
        } catch (MessagingException e) {
            logger.error("Failed to send alert email", e);
        }
    }

    /**
     * 检查邮件是否已配置
     * @return 是否已配置
     */
    private boolean isMailConfigured() {
        return StringUtils.hasText(BehaviorConfig.getMailHost()) &&
               StringUtils.hasText(BehaviorConfig.getMailFrom()) &&
               StringUtils.hasText(BehaviorConfig.getMailTo());
    }
}
