package cn.keking.audit.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_behavior_log", indexes = {
    @Index(name = "idx_ip", columnList = "ip"),
    @Index(name = "idx_request_time", columnList = "requestTime"),
    @Index(name = "idx_ip_time", columnList = "ip, requestTime")
})
public class UserBehaviorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String ip;

    @Column(length = 512)
    private String fileName;

    @Column(nullable = false)
    private LocalDateTime requestTime;

    @Column(length = 256)
    private String requestUri;

    @Column(length = 16)
    private String requestMethod;

    public UserBehaviorLog() {
    }

    public UserBehaviorLog(String ip, String fileName, LocalDateTime requestTime, String requestUri, String requestMethod) {
        this.ip = ip;
        this.fileName = fileName;
        this.requestTime = requestTime;
        this.requestUri = requestUri;
        this.requestMethod = requestMethod;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }
}
