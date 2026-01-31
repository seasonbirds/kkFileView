package cn.keking.service.userbehavior;

import cn.keking.config.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AlertEmailService {

    private static final Logger logger = LoggerFactory.getLogger(AlertEmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void sendAbnormalAccessAlert(String ipAddress, long requestCount, int periodMinutes, int maxRequests) {
        if (!ConfigConstants.isUserBehaviorAlertEmailEnabled()) {
            return;
        }
        
        String receiver = ConfigConstants.getUserBehaviorAlertEmailReceiver();
        if (receiver == null || receiver.trim().isEmpty()) {
            logger.warn("告警邮件接收者未配置，跳过发送");
            return;
        }

        String subject = buildSubject(ipAddress);
        String content = buildContent(ipAddress, requestCount, periodMinutes, maxRequests);

        sendEmail(receiver, subject, content);
    }

    public void sendDailyLimitExceededAlert(String ipAddress, long todayCount, int dailyLimit) {
        if (!ConfigConstants.isUserBehaviorAlertEmailEnabled()) {
            return;
        }

        String receiver = ConfigConstants.getUserBehaviorAlertEmailReceiver();
        if (receiver == null || receiver.trim().isEmpty()) {
            logger.warn("告警邮件接收者未配置，跳过发送");
            return;
        }

        String subject = "用户行为异常";
        String content = buildDailyLimitContent(ipAddress, todayCount, dailyLimit);

        sendEmail(receiver, subject, content);
    }

    private String buildSubject(String ipAddress) {
        return "用户行为异常";
    }

    private String buildContent(String ipAddress, long requestCount, int periodMinutes, int maxRequests) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style=\"font-family: Arial, sans-serif;\">");
        sb.append("<h2 style=\"color: #d9534f;\">⚠️ kkFileView 异常访问告警</h2>");
        sb.append("<table border=\"1\" cellpadding=\"10\" cellspacing=\"0\" style=\"border-collapse: collapse; width: 100%; max-width: 600px;\">");
        
        sb.append("<tr><td style=\"background-color: #f5f5f5; width: 30%;\"><strong>告警时间</strong></td>");
        sb.append("<td>").append(LocalDateTime.now().format(formatter)).append("</td></tr>");
        
        sb.append("<tr><td style=\"background-color: #f5f5f5;\"><strong>异常IP</strong></td>");
        sb.append("<td>").append(ipAddress).append("</td></tr>");
        
        sb.append("<tr><td style=\"background-color: #f5f5f5;\"><strong>统计周期</strong></td>");
        sb.append("<td>").append(periodMinutes).append(" 分钟</td></tr>");
        
        sb.append("<tr><td style=\"background-color: #f5f5f5;\"><strong>请求次数</strong></td>");
        sb.append("<td style=\"color: #d9534f;\"><strong>").append(requestCount).append("</strong> / ").append(maxRequests).append("</td></tr>");
        
        sb.append("<tr><td style=\"background-color: #f5f5f5;\"><strong>告警级别</strong></td>");
        sb.append("<td style=\"color: #d9534f;\"><strong>警告</strong></td></tr>");
        
        sb.append("<tr><td style=\"background-color: #f5f5f5;\"><strong>处理状态</strong></td>");
        sb.append("<td>该IP已被临时限制访问</td></tr>");
        
        sb.append("</table>");
        sb.append("<p style=\"margin-top: 20px; color: #666;\">IP地址").append(ipAddress).append("在过去").append(periodMinutes).append("分钟内访问了").append(requestCount).append("次系统，超出正常范围，请保持关注。</p>");
        sb.append("<p style=\"color: #666;\">如有疑问，请检查系统日志或联系管理员。</p>");
        sb.append("<hr style=\"margin-top: 30px;\">");
        sb.append("<p style=\"color: #999; font-size: 12px;\">此邮件由 kkFileView 系统自动发送，请勿回复。</p>");
        sb.append("</body></html>");
        
        return sb.toString();
    }

    private String buildDailyLimitContent(String ipAddress, long todayCount, int dailyLimit) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style=\"font-family: Arial, sans-serif;\">");
        sb.append("<h2 style=\"color: #d9534f;\">⚠️ kkFileView 日访问限额告警</h2>");
        sb.append("<table border=\"1\" cellpadding=\"10\" cellspacing=\"0\" style=\"border-collapse: collapse; width: 100%; max-width: 600px;\">");
        
        sb.append("<tr><td style=\"background-color: #f5f5f5; width: 30%;\"><strong>告警时间</strong></td>");
        sb.append("<td>").append(LocalDateTime.now().format(formatter)).append("</td></tr>");
        
        sb.append("<tr><td style=\"background-color: #f5f5f5;\"><strong>异常IP</strong></td>");
        sb.append("<td>").append(ipAddress).append("</td></tr>");
        
        sb.append("<tr><td style=\"background-color: #f5f5f5;\"><strong>今日访问次数</strong></td>");
        sb.append("<td style=\"color: #d9534f;\"><strong>").append(todayCount).append("</strong> / ").append(dailyLimit).append("</td></tr>");
        
        sb.append("<tr><td style=\"background-color: #f5f5f5;\"><strong>告警级别</strong></td>");
        sb.append("<td style=\"color: #d9534f;\"><strong>警告</strong></td></tr>");
        
        sb.append("<tr><td style=\"background-color: #f5f5f5;\"><strong>处理状态</strong></td>");
        sb.append("<td>该IP今日访问已被限制（次日0点自动解除）</td></tr>");
        
        sb.append("</table>");
        sb.append("<p style=\"margin-top: 20px; color: #666;\">IP地址").append(ipAddress).append("今日访问了").append(todayCount).append("次系统，已超出每日限额，请保持关注。</p>");
        sb.append("<p style=\"color: #666;\">如有疑问，请检查系统日志或联系管理员。</p>");
        sb.append("<hr style=\"margin-top: 30px;\">");
        sb.append("<p style=\"color: #999; font-size: 12px;\">此邮件由 kkFileView 系统自动发送，请勿回复。</p>");
        sb.append("</body></html>");
        
        return sb.toString();
    }

    private void sendEmail(String receiver, String subject, String content) {
        if (mailSender == null) {
            logger.warn("JavaMailSender 未配置，跳过发送告警邮件");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            String senderName = ConfigConstants.getUserBehaviorAlertEmailSenderName();
            String senderEmail = ConfigConstants.getMailUsername();
            
            if (senderEmail != null && !senderEmail.trim().isEmpty()) {
                if (senderName != null && !senderName.trim().isEmpty()) {
                    helper.setFrom(senderEmail, senderName);
                } else {
                    helper.setFrom(senderEmail);
                }
            }
            
            helper.setTo(receiver);
            helper.setSubject(subject);
            helper.setText(content, true);
            
            mailSender.send(message);
            logger.info("告警邮件已发送至: {}", receiver);
            
        } catch (MailException | MessagingException e) {
            logger.error("发送告警邮件失败: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("发送告警邮件时发生异常: {}", e.getMessage());
        }
    }
}
