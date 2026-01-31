package cn.keking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户行为记录实体类
 * 用于记录用户的预览请求信息
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
    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    /**
     * 请求的文件URL或名称
     */
    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    /**
     * 请求时间
     */
    @Column(name = "request_time", nullable = false)
    private LocalDateTime requestTime;

    /**
     * 用户代理信息
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 是否被标记为异常行为
     */
    @Column(name = "is_abnormal")
    private Boolean isAbnormal = false;

    /**
     * 异常行为描述
     */
    @Column(name = "abnormal_description", length = 500)
    private String abnormalDescription;

    public UserBehavior() {
    }

    public UserBehavior(String ipAddress, String fileUrl, LocalDateTime requestTime, String userAgent) {
        this.ipAddress = ipAddress;
        this.fileUrl = fileUrl;
        this.requestTime = requestTime;
        this.userAgent = userAgent;
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

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
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

    public Boolean getIsAbnormal() {
        return isAbnormal;
    }

    public void setIsAbnormal(Boolean abnormal) {
        isAbnormal = abnormal;
    }

    public String getAbnormalDescription() {
        return abnormalDescription;
    }

    public void setAbnormalDescription(String abnormalDescription) {
        this.abnormalDescription = abnormalDescription;
    }
}