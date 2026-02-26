package cn.keking.model;

import java.time.LocalDateTime;

/**
 * 用户行为日志实体类
 * @author kkfileview
 */
public class UserBehaviorLog {
    
    private Long id;
    private String ipAddress;
    private String fileName;
    private LocalDateTime requestTime;
    private String requestUrl;
    
    public UserBehaviorLog() {
    }
    
    public UserBehaviorLog(String ipAddress, String fileName, LocalDateTime requestTime, String requestUrl) {
        this.ipAddress = ipAddress;
        this.fileName = fileName;
        this.requestTime = requestTime;
        this.requestUrl = requestUrl;
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
    
    public String getRequestUrl() {
        return requestUrl;
    }
    
    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }
}
