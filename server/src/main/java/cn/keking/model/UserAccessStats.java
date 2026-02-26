package cn.keking.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户访问统计实体类
 * @author kkfileview
 */
public class UserAccessStats {
    
    private Long id;
    private String ipAddress;
    private LocalDate accessDate;
    private Integer requestCount;
    private Integer alertCount;
    private LocalDateTime firstAccessTime;
    private LocalDateTime lastAccessTime;
    private Boolean blocked;
    
    public UserAccessStats() {
        this.requestCount = 0;
        this.alertCount = 0;
        this.blocked = false;
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
    
    public LocalDate getAccessDate() {
        return accessDate;
    }
    
    public void setAccessDate(LocalDate accessDate) {
        this.accessDate = accessDate;
    }
    
    public Integer getRequestCount() {
        return requestCount;
    }
    
    public void setRequestCount(Integer requestCount) {
        this.requestCount = requestCount;
    }
    
    public Integer getAlertCount() {
        return alertCount;
    }
    
    public void setAlertCount(Integer alertCount) {
        this.alertCount = alertCount;
    }
    
    public LocalDateTime getFirstAccessTime() {
        return firstAccessTime;
    }
    
    public void setFirstAccessTime(LocalDateTime firstAccessTime) {
        this.firstAccessTime = firstAccessTime;
    }
    
    public LocalDateTime getLastAccessTime() {
        return lastAccessTime;
    }
    
    public void setLastAccessTime(LocalDateTime lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }
    
    public Boolean getBlocked() {
        return blocked;
    }
    
    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }
}
