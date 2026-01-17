package cn.keking.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Value("${security.behavior.enabled:false}")
    private boolean enabled;

    @Value("${security.behavior.period.minutes:1}")
    private int periodMinutes;

    @Value("${security.behavior.period.max.requests:60}")
    private int periodMaxRequests;

    @Value("${security.behavior.daily.max.requests:1000}")
    private int dailyMaxRequests;

    @Value("${security.behavior.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${security.behavior.email.from:}")
    private String emailFrom;

    @Value("${security.behavior.email.to:}")
    private String emailTo;

    @Value("${security.behavior.email.host:}")
    private String emailHost;

    @Value("${security.behavior.email.port:587}")
    private int emailPort;

    @Value("${security.behavior.email.username:}")
    private String emailUsername;

    @Value("${security.behavior.email.password:}")
    private String emailPassword;

    @Value("${security.behavior.database.path:./data/user_behavior.db}")
    private String databasePath;

    public boolean isEnabled() {
        return enabled;
    }

    public int getPeriodMinutes() {
        return periodMinutes;
    }

    public int getPeriodMaxRequests() {
        return periodMaxRequests;
    }

    public int getDailyMaxRequests() {
        return dailyMaxRequests;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public String getEmailTo() {
        return emailTo;
    }

    public String getEmailHost() {
        return emailHost;
    }

    public int getEmailPort() {
        return emailPort;
    }

    public String getEmailUsername() {
        return emailUsername;
    }

    public String getEmailPassword() {
        return emailPassword;
    }

    public String getDatabasePath() {
        return databasePath;
    }
}
