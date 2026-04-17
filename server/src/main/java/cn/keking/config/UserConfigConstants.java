package cn.keking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = UserConfigConstants.BEAN_NAME)
public class UserConfigConstants {
    public static final String BEAN_NAME = "userConfigConstants";

    private static Boolean userAuthEnabled;
    private static String defaultUserPhone;
    private static String defaultUserPassword;
    private static String defaultUserEmail;
    private static Integer tokenExpireMinutes;

    public static final String DEFAULT_USER_AUTH_ENABLED = "false";
    public static final String DEFAULT_USER_PHONE = "13800138000";
    public static final String DEFAULT_USER_PASSWORD = "123456";
    public static final String DEFAULT_USER_EMAIL = "admin@example.com";
    public static final String DEFAULT_TOKEN_EXPIRE_MINUTES = "1440";

    public static Boolean isUserAuthEnabled() {
        return userAuthEnabled;
    }

    @Value("${user.auth.enabled:false}")
    public void setUserAuthEnabled(String userAuthEnabled) {
        setUserAuthEnabledValue(Boolean.parseBoolean(userAuthEnabled));
    }

    public static void setUserAuthEnabledValue(Boolean userAuthEnabled) {
        UserConfigConstants.userAuthEnabled = userAuthEnabled;
    }

    public static String getDefaultUserPhone() {
        return defaultUserPhone;
    }

    @Value("${user.default.phone:13800138000}")
    public void setDefaultUserPhone(String defaultUserPhone) {
        setDefaultUserPhoneValue(defaultUserPhone);
    }

    public static void setDefaultUserPhoneValue(String defaultUserPhone) {
        UserConfigConstants.defaultUserPhone = defaultUserPhone;
    }

    public static String getDefaultUserPassword() {
        return defaultUserPassword;
    }

    @Value("${user.default.password:123456}")
    public void setDefaultUserPassword(String defaultUserPassword) {
        setDefaultUserPasswordValue(defaultUserPassword);
    }

    public static void setDefaultUserPasswordValue(String defaultUserPassword) {
        UserConfigConstants.defaultUserPassword = defaultUserPassword;
    }

    public static String getDefaultUserEmail() {
        return defaultUserEmail;
    }

    @Value("${user.default.email:admin@example.com}")
    public void setDefaultUserEmail(String defaultUserEmail) {
        setDefaultUserEmailValue(defaultUserEmail);
    }

    public static void setDefaultUserEmailValue(String defaultUserEmail) {
        UserConfigConstants.defaultUserEmail = defaultUserEmail;
    }

    public static Integer getTokenExpireMinutes() {
        return tokenExpireMinutes;
    }

    @Value("${user.token.expire.minutes:1440}")
    public void setTokenExpireMinutes(Integer tokenExpireMinutes) {
        setTokenExpireMinutesValue(tokenExpireMinutes);
    }

    public static void setTokenExpireMinutesValue(Integer tokenExpireMinutes) {
        UserConfigConstants.tokenExpireMinutes = tokenExpireMinutes;
    }
}
