package cn.keking.audit.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 用户行为审计配置类
 * 用于管理审计功能的各项配置参数
 */
@Component
public class AuditConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuditConfig.class);

    private static Integer timeWindowMinutes;
    private static Integer maxRequestsPerWindow;
    private static Integer maxRequestsPerDay;
    private static String adminEmail;
    private static String emailFrom;
    private static String emailHost;
    private static Integer emailPort;
    private static String emailUsername;
    private static String emailPassword;
    private static Boolean emailAuthEnabled;
    private static Boolean emailStartTlsEnabled;
    private static Integer alertCooldownMinutes;

    public static final String DEFAULT_TIME_WINDOW_MINUTES = "10";
    public static final String DEFAULT_MAX_REQUESTS_PER_WINDOW = "100";
    public static final String DEFAULT_MAX_REQUESTS_PER_DAY = "1000";
    public static final String DEFAULT_ALERT_COOLDOWN_MINUTES = "60";
    public static final String DEFAULT_EMAIL_PORT = "25";
    public static final String DEFAULT_EMAIL_AUTH_ENABLED = "false";
    public static final String DEFAULT_EMAIL_STARTTLS_ENABLED = "false";

    @PostConstruct
    public void init() {
        logger.info("AuditConfig initialized with: timeWindow={}min, maxPerWindow={}, maxPerDay={}",
                timeWindowMinutes, maxRequestsPerWindow, maxRequestsPerDay);
    }

    /**
     * 获取统计周期（分钟）
     *
     * @return 统计周期分钟数
     */
    public static Integer getTimeWindowMinutes() {
        return timeWindowMinutes != null ? timeWindowMinutes : Integer.parseInt(DEFAULT_TIME_WINDOW_MINUTES);
    }

    @Value("${audit.time.window.minutes:10}")
    public void setTimeWindowMinutes(Integer timeWindowMinutes) {
        AuditConfig.timeWindowMinutes = timeWindowMinutes;
    }

    /**
     * 获取统计周期内最大访问次数
     *
     * @return 最大访问次数
     */
    public static Integer getMaxRequestsPerWindow() {
        return maxRequestsPerWindow != null ? maxRequestsPerWindow : Integer.parseInt(DEFAULT_MAX_REQUESTS_PER_WINDOW);
    }

    @Value("${audit.max.requests.per.window:100}")
    public void setMaxRequestsPerWindow(Integer maxRequestsPerWindow) {
        AuditConfig.maxRequestsPerWindow = maxRequestsPerWindow;
    }

    /**
     * 获取每日最大访问次数
     *
     * @return 每日最大访问次数
     */
    public static Integer getMaxRequestsPerDay() {
        return maxRequestsPerDay != null ? maxRequestsPerDay : Integer.parseInt(DEFAULT_MAX_REQUESTS_PER_DAY);
    }

    @Value("${audit.max.requests.per.day:1000}")
    public void setMaxRequestsPerDay(Integer maxRequestsPerDay) {
        AuditConfig.maxRequestsPerDay = maxRequestsPerDay;
    }

    /**
     * 获取管理员邮箱地址
     *
     * @return 管理员邮箱
     */
    public static String getAdminEmail() {
        return adminEmail;
    }

    @Value("${audit.admin.email:}")
    public void setAdminEmail(String adminEmail) {
        AuditConfig.adminEmail = adminEmail;
    }

    /**
     * 获取发件人邮箱地址
     *
     * @return 发件人邮箱
     */
    public static String getEmailFrom() {
        return emailFrom;
    }

    @Value("${audit.email.from:}")
    public void setEmailFrom(String emailFrom) {
        AuditConfig.emailFrom = emailFrom;
    }

    /**
     * 获取SMTP服务器地址
     *
     * @return SMTP服务器地址
     */
    public static String getEmailHost() {
        return emailHost;
    }

    @Value("${audit.email.host:}")
    public void setEmailHost(String emailHost) {
        AuditConfig.emailHost = emailHost;
    }

    /**
     * 获取SMTP服务器端口
     *
     * @return SMTP端口
     */
    public static Integer getEmailPort() {
        return emailPort != null ? emailPort : Integer.parseInt(DEFAULT_EMAIL_PORT);
    }

    @Value("${audit.email.port:25}")
    public void setEmailPort(Integer emailPort) {
        AuditConfig.emailPort = emailPort;
    }

    /**
     * 获取SMTP用户名
     *
     * @return SMTP用户名
     */
    public static String getEmailUsername() {
        return emailUsername;
    }

    @Value("${audit.email.username:}")
    public void setEmailUsername(String emailUsername) {
        AuditConfig.emailUsername = emailUsername;
    }

    /**
     * 获取SMTP密码
     *
     * @return SMTP密码
     */
    public static String getEmailPassword() {
        return emailPassword;
    }

    @Value("${audit.email.password:}")
    public void setEmailPassword(String emailPassword) {
        AuditConfig.emailPassword = emailPassword;
    }

    /**
     * 是否启用SMTP认证
     *
     * @return true表示启用认证
     */
    public static Boolean isEmailAuthEnabled() {
        return emailAuthEnabled != null ? emailAuthEnabled : Boolean.parseBoolean(DEFAULT_EMAIL_AUTH_ENABLED);
    }

    @Value("${audit.email.auth.enabled:false}")
    public void setEmailAuthEnabled(Boolean emailAuthEnabled) {
        AuditConfig.emailAuthEnabled = emailAuthEnabled;
    }

    /**
     * 是否启用STARTTLS
     *
     * @return true表示启用STARTTLS
     */
    public static Boolean isEmailStartTlsEnabled() {
        return emailStartTlsEnabled != null ? emailStartTlsEnabled : Boolean.parseBoolean(DEFAULT_EMAIL_STARTTLS_ENABLED);
    }

    @Value("${audit.email.starttls.enabled:false}")
    public void setEmailStartTlsEnabled(Boolean emailStartTlsEnabled) {
        AuditConfig.emailStartTlsEnabled = emailStartTlsEnabled;
    }

    /**
     * 获取告警邮件冷却时间（分钟）
     * 同一IP在冷却时间内不会重复发送告警邮件
     *
     * @return 冷却时间分钟数
     */
    public static Integer getAlertCooldownMinutes() {
        return alertCooldownMinutes != null ? alertCooldownMinutes : Integer.parseInt(DEFAULT_ALERT_COOLDOWN_MINUTES);
    }

    @Value("${audit.alert.cooldown.minutes:60}")
    public void setAlertCooldownMinutes(Integer alertCooldownMinutes) {
        AuditConfig.alertCooldownMinutes = alertCooldownMinutes;
    }
}
