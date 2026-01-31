package cn.keking.service.userbehavior;

public class UserBehavior {
    
    private Long id;
    private String ipAddress;
    private String fileName;
    private Long requestTime;
    private String requestDate;
    
    public UserBehavior() {
    }
    
    public UserBehavior(String ipAddress, String fileName, Long requestTime, String requestDate) {
        this.ipAddress = ipAddress;
        this.fileName = fileName;
        this.requestTime = requestTime;
        this.requestDate = requestDate;
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
    
    public Long getRequestTime() {
        return requestTime;
    }
    
    public void setRequestTime(Long requestTime) {
        this.requestTime = requestTime;
    }
    
    public String getRequestDate() {
        return requestDate;
    }
    
    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }
}
