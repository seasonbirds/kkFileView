package cn.keking.model.monitor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户行为记录实体类
 */
@Entity
@Table(name = "user_behavior")
public class UserBehavior {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户IP地址
     */
    @Column(nullable = false, length = 45)
    private String ipAddress;

    /**
     * 请求的文件名
     */
    @Column(length = 500)
    private String fileName;

    /**
     * 请求的URL
     */
    @Column(length = 1000)
    private String requestUrl;

    /**
     * 请求时间
     */
    @Column(nullable = false)
    private LocalDateTime requestTime;

    /**
     * 用户代理
     */
    @Column(length = 1000)
    private String userAgent;

    /**
     * 请求是否被阻止
     */
    @Column(nullable = false)
    private Boolean blocked = false;

    public UserBehavior() {
    }

    public UserBehavior(String ipAddress, String fileName, String requestUrl, String userAgent) {
        this.ipAddress = ipAddress;
        this.fileName = fileName;
        this.requestUrl = requestUrl;
        this.userAgent = userAgent;
        this.requestTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }
}
