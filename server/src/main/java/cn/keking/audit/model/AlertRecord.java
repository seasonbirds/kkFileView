package cn.keking.audit.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_record", indexes = {
    @Index(name = "idx_alert_ip", columnList = "ip"),
    @Index(name = "idx_alert_time", columnList = "alertTime")
})
public class AlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String ip;

    @Column(nullable = false)
    private Integer accessCount;

    @Column(nullable = false)
    private Integer timeWindowMinutes;

    @Column(nullable = false)
    private LocalDateTime alertTime;

    @Column(length = 512)
    private String alertMessage;

    @Column(length = 32)
    private String alertType;

    public AlertRecord() {
    }

    public AlertRecord(String ip, Integer accessCount, Integer timeWindowMinutes, 
                       LocalDateTime alertTime, String alertMessage, String alertType) {
        this.ip = ip;
        this.accessCount = accessCount;
        this.timeWindowMinutes = timeWindowMinutes;
        this.alertTime = alertTime;
        this.alertMessage = alertMessage;
        this.alertType = alertType;
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

    public Integer getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Integer accessCount) {
        this.accessCount = accessCount;
    }

    public Integer getTimeWindowMinutes() {
        return timeWindowMinutes;
    }

    public void setTimeWindowMinutes(Integer timeWindowMinutes) {
        this.timeWindowMinutes = timeWindowMinutes;
    }

    public LocalDateTime getAlertTime() {
        return alertTime;
    }

    public void setAlertTime(LocalDateTime alertTime) {
        this.alertTime = alertTime;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }
}
