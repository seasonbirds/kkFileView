package cn.keking.security.model;

import java.time.LocalDateTime;

public class UserBehaviorLog {
    private Long id;
    private String ipAddress;
    private String fileName;
    private LocalDateTime requestTime;

    public UserBehaviorLog() {}

    public UserBehaviorLog(String ipAddress, String fileName, LocalDateTime requestTime) {
        this.ipAddress = ipAddress;
        this.fileName = fileName;
        this.requestTime = requestTime;
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

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }
}
