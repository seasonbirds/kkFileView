package cn.keking.behavior.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 用户行为监控配置类
 * 管理所有行为监控相关的配置项
 */
@Component
public class BehaviorConfig {

    private static boolean enabled;
    private static int periodMinutes;
    private static int periodThreshold;
    private static int dailyThreshold;
    private static String dbPath;

    private static String mailHost;
    private static int mailPort;
    private static String mailUsername;
    private static String mailPassword;
    private static String mailFrom;
    private static String mailTo;
    private static boolean mailSslEnabled;

    private static String mailPeriodAlertSubject;
    private static String mailPeriodAlertContent;
    private static String mailDailyAlertSubject;
    private static String mailDailyAlertContent;

    public static final int DEFAULT_PERIOD_MINUTES = 5;
    public static final int DEFAULT_PERIOD_THRESHOLD = 100;
    public static final int DEFAULT_DAILY_THRESHOLD = 1000;

    /**
     * 是否启用行为监控功能
     */
    public static boolean isEnabled() {
        return enabled;
    }

    @Value("${behavior.enabled:false}")
    public void setEnabled(boolean enabled) {
        BehaviorConfig.enabled = enabled;
    }

    /**
     * 获取统计周期（分钟）
     */
    public static int getPeriodMinutes() {
        return periodMinutes;
    }

    @Value("${behavior.period.minutes:5}")
    public void setPeriodMinutes(int periodMinutes) {
        BehaviorConfig.periodMinutes = periodMinutes;
    }

    /**
     * 获取周期内访问阈值
     */
    public static int getPeriodThreshold() {
        return periodThreshold;
    }

    @Value("${behavior.period.threshold:100}")
    public void setPeriodThreshold(int periodThreshold) {
        BehaviorConfig.periodThreshold = periodThreshold;
    }

    /**
     * 获取每日访问阈值
     */
    public static int getDailyThreshold() {
        return dailyThreshold;
    }

    @Value("${behavior.daily.threshold:1000}")
    public void setDailyThreshold(int dailyThreshold) {
        BehaviorConfig.dailyThreshold = dailyThreshold;
    }

    /**
     * 获取数据库存储路径
     */
    public static String getDbPath() {
        return dbPath;
    }

    @Value("${behavior.db.path:${user.home}/kkFileView/behavior.db}")
    public void setDbPath(String dbPath) {
        BehaviorConfig.dbPath = dbPath;
    }

    /**
     * 获取SMTP服务器地址
     */
    public static String getMailHost() {
        return mailHost;
    }

    @Value("${behavior.mail.host:}")
    public void setMailHost(String mailHost) {
        BehaviorConfig.mailHost = mailHost;
    }

    /**
     * 获取SMTP服务器端口
     */
    public static int getMailPort() {
        return mailPort;
    }

    @Value("${behavior.mail.port:465}")
    public void setMailPort(int mailPort) {
        BehaviorConfig.mailPort = mailPort;
    }

    /**
     * 获取SMTP用户名
     */
    public static String getMailUsername() {
        return mailUsername;
    }

    @Value("${behavior.mail.username:}")
    public void setMailUsername(String mailUsername) {
        BehaviorConfig.mailUsername = mailUsername;
    }

    /**
     * 获取SMTP密码
     */
    public static String getMailPassword() {
        return mailPassword;
    }

    @Value("${behavior.mail.password:}")
    public void setMailPassword(String mailPassword) {
        BehaviorConfig.mailPassword = mailPassword;
    }

    /**
     * 获取发件人邮箱
     */
    public static String getMailFrom() {
        return mailFrom;
    }

    @Value("${behavior.mail.from:}")
    public void setMailFrom(String mailFrom) {
        BehaviorConfig.mailFrom = mailFrom;
    }

    /**
     * 获取收件人邮箱（管理员邮箱）
     */
    public static String getMailTo() {
        return mailTo;
    }

    @Value("${behavior.mail.to:}")
    public void setMailTo(String mailTo) {
        BehaviorConfig.mailTo = mailTo;
    }

    /**
     * 是否启用SSL
     */
    public static boolean isMailSslEnabled() {
        return mailSslEnabled;
    }

    @Value("${behavior.mail.ssl.enabled:true}")
    public void setMailSslEnabled(boolean mailSslEnabled) {
        BehaviorConfig.mailSslEnabled = mailSslEnabled;
    }

    /**
     * 获取周期告警邮件标题
     */
    public static String getMailPeriodAlertSubject() {
        return mailPeriodAlertSubject;
    }

    @Value("${behavior.mail.period.alert.subject:用户行为异常}")
    public void setMailPeriodAlertSubject(String mailPeriodAlertSubject) {
        BehaviorConfig.mailPeriodAlertSubject = mailPeriodAlertSubject;
    }

    /**
     * 获取周期告警邮件内容模板
     * 支持占位符：{ip}、{minutes}、{count}
     */
    public static String getMailPeriodAlertContent() {
        return mailPeriodAlertContent;
    }

    @Value("${behavior.mail.period.alert.content:IP地址 {ip} 在过去 {minutes} 分钟内访问了 {count} 次系统，超出正常范围，请保持关注。}")
    public void setMailPeriodAlertContent(String mailPeriodAlertContent) {
        BehaviorConfig.mailPeriodAlertContent = mailPeriodAlertContent;
    }

    /**
     * 获取每日告警邮件标题
     */
    public static String getMailDailyAlertSubject() {
        return mailDailyAlertSubject;
    }

    @Value("${behavior.mail.daily.alert.subject:用户行为异常 - 日访问量超限}")
    public void setMailDailyAlertSubject(String mailDailyAlertSubject) {
        BehaviorConfig.mailDailyAlertSubject = mailDailyAlertSubject;
    }

    /**
     * 获取每日告警邮件内容模板
     * 支持占位符：{ip}、{count}
     */
    public static String getMailDailyAlertContent() {
        return mailDailyAlertContent;
    }

    @Value("${behavior.mail.daily.alert.content:IP地址 {ip} 今日已访问 {count} 次系统，超出每日访问限制，已被禁止访问至明日零点，请保持关注。}")
    public void setMailDailyAlertContent(String mailDailyAlertContent) {
        BehaviorConfig.mailDailyAlertContent = mailDailyAlertContent;
    }
}
