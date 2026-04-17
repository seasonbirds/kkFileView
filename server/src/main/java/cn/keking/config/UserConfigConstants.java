package cn.keking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 用户模块配置常量类
 * 存储用户模块相关的配置项，包括默认用户信息和Token过期时间
 */
@Component(value = UserConfigConstants.BEAN_NAME)
public class UserConfigConstants {
    public static final String BEAN_NAME = "userConfigConstants";

    /**
     * 默认用户手机号
     */
    private static String defaultUserPhone;

    /**
     * 默认用户密码
     */
    private static String defaultUserPassword;

    /**
     * 默认用户邮箱
     */
    private static String defaultUserEmail;

    /**
     * Token过期时间（分钟）
     */
    private static Integer tokenExpireMinutes;

    /**
     * 默认用户手机号默认值
     */
    public static final String DEFAULT_USER_PHONE = "13800138000";

    /**
     * 默认用户密码默认值
     */
    public static final String DEFAULT_USER_PASSWORD = "123456";

    /**
     * 默认用户邮箱默认值
     */
    public static final String DEFAULT_USER_EMAIL = "admin@example.com";

    /**
     * Token过期时间默认值（24小时，单位：分钟）
     */
    public static final String DEFAULT_TOKEN_EXPIRE_MINUTES = "1440";

    /**
     * 获取默认用户手机号
     * @return 默认用户手机号
     */
    public static String getDefaultUserPhone() {
        return defaultUserPhone;
    }

    /**
     * 设置默认用户手机号（通过Spring @Value注入）
     * @param defaultUserPhone 默认用户手机号
     */
    @Value("${user.default.phone:13800138000}")
    public void setDefaultUserPhone(String defaultUserPhone) {
        setDefaultUserPhoneValue(defaultUserPhone);
    }

    /**
     * 静态方法设置默认用户手机号
     * @param defaultUserPhone 默认用户手机号
     */
    public static void setDefaultUserPhoneValue(String defaultUserPhone) {
        UserConfigConstants.defaultUserPhone = defaultUserPhone;
    }

    /**
     * 获取默认用户密码
     * @return 默认用户密码
     */
    public static String getDefaultUserPassword() {
        return defaultUserPassword;
    }

    /**
     * 设置默认用户密码（通过Spring @Value注入）
     * @param defaultUserPassword 默认用户密码
     */
    @Value("${user.default.password:123456}")
    public void setDefaultUserPassword(String defaultUserPassword) {
        setDefaultUserPasswordValue(defaultUserPassword);
    }

    /**
     * 静态方法设置默认用户密码
     * @param defaultUserPassword 默认用户密码
     */
    public static void setDefaultUserPasswordValue(String defaultUserPassword) {
        UserConfigConstants.defaultUserPassword = defaultUserPassword;
    }

    /**
     * 获取默认用户邮箱
     * @return 默认用户邮箱
     */
    public static String getDefaultUserEmail() {
        return defaultUserEmail;
    }

    /**
     * 设置默认用户邮箱（通过Spring @Value注入）
     * @param defaultUserEmail 默认用户邮箱
     */
    @Value("${user.default.email:admin@example.com}")
    public void setDefaultUserEmail(String defaultUserEmail) {
        setDefaultUserEmailValue(defaultUserEmail);
    }

    /**
     * 静态方法设置默认用户邮箱
     * @param defaultUserEmail 默认用户邮箱
     */
    public static void setDefaultUserEmailValue(String defaultUserEmail) {
        UserConfigConstants.defaultUserEmail = defaultUserEmail;
    }

    /**
     * 获取Token过期时间
     * @return Token过期时间（分钟）
     */
    public static Integer getTokenExpireMinutes() {
        return tokenExpireMinutes;
    }

    /**
     * 设置Token过期时间（通过Spring @Value注入）
     * @param tokenExpireMinutes Token过期时间（分钟）
     */
    @Value("${user.token.expire.minutes:1440}")
    public void setTokenExpireMinutes(Integer tokenExpireMinutes) {
        setTokenExpireMinutesValue(tokenExpireMinutes);
    }

    /**
     * 静态方法设置Token过期时间
     * @param tokenExpireMinutes Token过期时间（分钟）
     */
    public static void setTokenExpireMinutesValue(Integer tokenExpireMinutes) {
        UserConfigConstants.tokenExpireMinutes = tokenExpireMinutes;
    }
}
