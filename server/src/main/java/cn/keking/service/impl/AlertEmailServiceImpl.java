package cn.keking.service.impl;

import cn.keking.service.AlertEmailService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Properties;

/**
 * 告警邮件服务实现类
 * @author kkfileview
 */
@Service
public class AlertEmailServiceImpl implements AlertEmailService {

    private static final Logger logger = LoggerFactory.getLogger(AlertEmailServiceImpl.class);

    @Value("${behavior.monitor.alert.email.enabled:false}")
    private Boolean emailEnabled;

    @Value("${behavior.monitor.alert.email.host:}")
    private String emailHost;

    @Value("${behavior.monitor.alert.email.port:25}")
    private Integer emailPort;

    @Value("${behavior.monitor.alert.email.username:}")
    private String emailUsername;

    @Value("${behavior.monitor.alert.email.password:}")
    private String emailPassword;

    @Value("${behavior.monitor.alert.email.from:}")
    private String emailFrom;

    @Value("${behavior.monitor.alert.email.to:}")
    private String emailTo;

    @Value("${behavior.monitor.alert.email.subject:用户行为异常}")
    private String emailSubject;

    @Value("${behavior.monitor.alert.email.content:IP地址 %s 在过去 %d 分钟内访问了 %d 次系统，超出正常范围，请保持关注。}")
    private String emailContentTemplate;

    private JavaMailSender mailSender;

    @PostConstruct
    public void init() {
        if (Boolean.TRUE.equals(emailEnabled) && isEmailConfigValid()) {
            try {
                JavaMailSenderImpl sender = new JavaMailSenderImpl();
                sender.setHost(emailHost);
                sender.setPort(emailPort);
                sender.setUsername(emailUsername);
                sender.setPassword(emailPassword);

                Properties props = sender.getJavaMailProperties();
                props.put("mail.transport.protocol", "smtp");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.debug", "false");

                this.mailSender = sender;
                logger.info("告警邮件服务初始化成功");
            } catch (Exception e) {
                logger.error("告警邮件服务初始化失败", e);
            }
        } else {
            logger.info("告警邮件服务未启用或配置不完整");
        }
    }

    @Override
    public boolean isEmailConfigValid() {
        return StringUtils.isNotBlank(emailHost)
                && StringUtils.isNotBlank(emailUsername)
                && StringUtils.isNotBlank(emailPassword)
                && StringUtils.isNotBlank(emailTo);
    }

    @Override
    @Async("userBehaviorExecutor")
    public void sendAbnormalBehaviorAlert(String ipAddress, int windowMinutes, int requestCount) {
        if (!Boolean.TRUE.equals(emailEnabled) || mailSender == null) {
            logger.warn("邮件告警未启用或邮件服务未配置，跳过发送告警邮件");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(StringUtils.isNotBlank(emailFrom) ? emailFrom : emailUsername);
            message.setTo(emailTo.split(","));
            message.setSubject(emailSubject);

            String content = String.format(emailContentTemplate, ipAddress, windowMinutes, requestCount);
            message.setText(content);

            mailSender.send(message);
            logger.info("已发送用户行为异常告警邮件，IP: {}, 周期: {}分钟, 请求次数: {}",
                    ipAddress, windowMinutes, requestCount);
        } catch (Exception e) {
            logger.error("发送告警邮件失败", e);
        }
    }
}
