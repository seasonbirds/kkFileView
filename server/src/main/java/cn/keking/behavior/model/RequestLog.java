package cn.keking.behavior.model;

import java.time.LocalDateTime;

/**
 * 请求日志实体类
 * 记录用户预览请求的详细信息
 */
public class RequestLog {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 请求IP地址
     */
    private String ipAddress;

    /**
     * 预览的文件名称
     */
    private String fileName;

    /**
     * 请求时间
     */
    private LocalDateTime requestTime;

    /**
     * 默认构造函数
     */
    public RequestLog() {
    }

    /**
     * 带参数的构造函数
     * @param ipAddress IP地址
     * @param fileName 文件名
     * @param requestTime 请求时间
     */
    public RequestLog(String ipAddress, String fileName, LocalDateTime requestTime) {
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
