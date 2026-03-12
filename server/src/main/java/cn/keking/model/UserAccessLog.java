package cn.keking.model;

import java.util.Date;

public class UserAccessLog {
    private Long id;
    private String ipAddress;
    private String fileName;
    private Date requestTime;

    public UserAccessLog() {
    }

    public UserAccessLog(String ipAddress, String fileName, Date requestTime) {
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

    public Date getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Date requestTime) {
        this.requestTime = requestTime;
    }
}
