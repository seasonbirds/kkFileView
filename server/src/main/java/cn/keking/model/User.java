package cn.keking.model;

import java.io.Serializable;

/**
 * 用户实体类
 * 包含用户的基本信息：手机号、密码、邮箱和Token
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 手机号（用户登录名）
     */
    private String phone;

    /**
     * 密码
     */
    private String password;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 登录Token
     */
    private String token;

    /**
     * 默认构造函数
     */
    public User() {
    }

    /**
     * 带参数的构造函数
     * 
     * @param phone 手机号
     * @param password 密码
     * @param email 邮箱地址
     */
    public User(String phone, String password, String email) {
        this.phone = phone;
        this.password = password;
        this.email = email;
    }

    /**
     * 获取手机号
     * 
     * @return 手机号
     */
    public String getPhone() {
        return phone;
    }

    /**
     * 设置手机号
     * 
     * @param phone 手机号
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * 获取密码
     * 
     * @return 密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码
     * 
     * @param password 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取邮箱地址
     * 
     * @return 邮箱地址
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置邮箱地址
     * 
     * @param email 邮箱地址
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 获取登录Token
     * 
     * @return 登录Token
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置登录Token
     * 
     * @param token 登录Token
     */
    public void setToken(String token) {
        this.token = token;
    }
}
