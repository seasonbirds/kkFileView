package cn.keking.model;

import java.time.LocalDateTime;

public class UserBehavior {

    private Long id;
    private String ipAddress;
    private String fileName;
    private String requestUrl;
    private LocalDateTime requestTime;

    public UserBehavior() {
    }

    public UserBehavior(String ipAddress, String fileName, String requestUrl) {
        this.ipAddress = ipAddress;
        this.fileName = fileName;
        this.requestUrl = requestUrl;
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
}
