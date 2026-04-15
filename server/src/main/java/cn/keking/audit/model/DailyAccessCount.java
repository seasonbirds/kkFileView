package cn.keking.audit.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_access_count", indexes = {
    @Index(name = "idx_daily_ip", columnList = "ip"),
    @Index(name = "idx_daily_date", columnList = "accessDate"),
    @Index(name = "idx_daily_ip_date", columnList = "ip, accessDate", unique = true)
})
public class DailyAccessCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String ip;

    @Column(nullable = false)
    private LocalDate accessDate;

    @Column(nullable = false)
    private Integer accessCount = 0;

    public DailyAccessCount() {
    }

    public DailyAccessCount(String ip, LocalDate accessDate, Integer accessCount) {
        this.ip = ip;
        this.accessDate = accessDate;
        this.accessCount = accessCount;
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

    public LocalDate getAccessDate() {
        return accessDate;
    }

    public void setAccessDate(LocalDate accessDate) {
        this.accessDate = accessDate;
    }

    public Integer getAccessCount() {
        return accessCount;
    }

    public void setAccessCount(Integer accessCount) {
        this.accessCount = accessCount;
    }

    public void incrementCount() {
        this.accessCount++;
    }
}
