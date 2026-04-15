package cn.keking.audit.config;

import cn.keking.utils.ConfigUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class AuditConfig {

    private static final Logger logger = LoggerFactory.getLogger(AuditConfig.class);

    private static Boolean auditEnabled;
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
    private static Integer logRetentionDays;
    private static Integer alertCooldownMinutes;

    public static final String DEFAULT_AUDIT_ENABLED = "true";
    public static final String DEFAULT_TIME_WINDOW_MINUTES = "10";
    public static final String DEFAULT_MAX_REQUESTS_PER_WINDOW = "100";
    public static final String DEFAULT_MAX_REQUESTS_PER_DAY = "1000";
    public static final String DEFAULT_LOG_RETENTION_DAYS = "30";
    public static final String DEFAULT_ALERT_COOLDOWN_MINUTES = "60";
    public static final String DEFAULT_EMAIL_PORT = "25";
    public static final String DEFAULT_EMAIL_AUTH_ENABLED = "false";
    public static final String DEFAULT_EMAIL_STARTTLS_ENABLED = "false";

    @PostConstruct
    public void init() {
        logger.info("AuditConfig initialized with: auditEnabled={}, timeWindow={}min, maxPerWindow={}, maxPerDay={}",
                auditEnabled, timeWindowMinutes, maxRequestsPerWindow, maxRequestsPerDay);
    }

    public static String getDbPath() {
        String homePath = ConfigUtils.getHomePath();
        return homePath + File.separator + "data" + File.separator + "audit.db";
    }

    public static Boolean isAuditEnabled() {
        return auditEnabled != null ? auditEnabled : Boolean.parseBoolean(DEFAULT_AUDIT_ENABLED);
    }

    @Value("${audit.enabled:true}")
    public void setAuditEnabled(Boolean auditEnabled) {
        AuditConfig.auditEnabled = auditEnabled;
    }

    public static Integer getTimeWindowMinutes() {
        return timeWindowMinutes != null ? timeWindowMinutes : Integer.parseInt(DEFAULT_TIME_WINDOW_MINUTES);
    }

    @Value("${audit.time.window.minutes:10}")
    public void setTimeWindowMinutes(Integer timeWindowMinutes) {
        AuditConfig.timeWindowMinutes = timeWindowMinutes;
    }

    public static Integer getMaxRequestsPerWindow() {
        return maxRequestsPerWindow != null ? maxRequestsPerWindow : Integer.parseInt(DEFAULT_MAX_REQUESTS_PER_WINDOW);
    }

    @Value("${audit.max.requests.per.window:100}")
    public void setMaxRequestsPerWindow(Integer maxRequestsPerWindow) {
        AuditConfig.maxRequestsPerWindow = maxRequestsPerWindow;
    }

    public static Integer getMaxRequestsPerDay() {
        return maxRequestsPerDay != null ? maxRequestsPerDay : Integer.parseInt(DEFAULT_MAX_REQUESTS_PER_DAY);
    }

    @Value("${audit.max.requests.per.day:1000}")
    public void setMaxRequestsPerDay(Integer maxRequestsPerDay) {
        AuditConfig.maxRequestsPerDay = maxRequestsPerDay;
    }

    public static String getAdminEmail() {
        return adminEmail;
    }

    @Value("${audit.admin.email:}")
    public void setAdminEmail(String adminEmail) {
        AuditConfig.adminEmail = adminEmail;
    }

    public static String getEmailFrom() {
        return emailFrom;
    }

    @Value("${audit.email.from:}")
    public void setEmailFrom(String emailFrom) {
        AuditConfig.emailFrom = emailFrom;
    }

    public static String getEmailHost() {
        return emailHost;
    }

    @Value("${audit.email.host:}")
    public void setEmailHost(String emailHost) {
        AuditConfig.emailHost = emailHost;
    }

    public static Integer getEmailPort() {
        return emailPort != null ? emailPort : Integer.parseInt(DEFAULT_EMAIL_PORT);
    }

    @Value("${audit.email.port:25}")
    public void setEmailPort(Integer emailPort) {
        AuditConfig.emailPort = emailPort;
    }

    public static String getEmailUsername() {
        return emailUsername;
    }

    @Value("${audit.email.username:}")
    public void setEmailUsername(String emailUsername) {
        AuditConfig.emailUsername = emailUsername;
    }

    public static String getEmailPassword() {
        return emailPassword;
    }

    @Value("${audit.email.password:}")
    public void setEmailPassword(String emailPassword) {
        AuditConfig.emailPassword = emailPassword;
    }

    public static Boolean isEmailAuthEnabled() {
        return emailAuthEnabled != null ? emailAuthEnabled : Boolean.parseBoolean(DEFAULT_EMAIL_AUTH_ENABLED);
    }

    @Value("${audit.email.auth.enabled:false}")
    public void setEmailAuthEnabled(Boolean emailAuthEnabled) {
        AuditConfig.emailAuthEnabled = emailAuthEnabled;
    }

    public static Boolean isEmailStartTlsEnabled() {
        return emailStartTlsEnabled != null ? emailStartTlsEnabled : Boolean.parseBoolean(DEFAULT_EMAIL_STARTTLS_ENABLED);
    }

    @Value("${audit.email.starttls.enabled:false}")
    public void setEmailStartTlsEnabled(Boolean emailStartTlsEnabled) {
        AuditConfig.emailStartTlsEnabled = emailStartTlsEnabled;
    }

    public static Integer getLogRetentionDays() {
        return logRetentionDays != null ? logRetentionDays : Integer.parseInt(DEFAULT_LOG_RETENTION_DAYS);
    }

    @Value("${audit.log.retention.days:30}")
    public void setLogRetentionDays(Integer logRetentionDays) {
        AuditConfig.logRetentionDays = logRetentionDays;
    }

    public static Integer getAlertCooldownMinutes() {
        return alertCooldownMinutes != null ? alertCooldownMinutes : Integer.parseInt(DEFAULT_ALERT_COOLDOWN_MINUTES);
    }

    @Value("${audit.alert.cooldown.minutes:60}")
    public void setAlertCooldownMinutes(Integer alertCooldownMinutes) {
        AuditConfig.alertCooldownMinutes = alertCooldownMinutes;
    }
}
