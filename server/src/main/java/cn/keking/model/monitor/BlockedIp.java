package cn.keking.model.monitor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 被封禁IP实体类
 */
@Entity
@Table(name = "blocked_ip")
public class BlockedIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * IP地址
     */
    @Column(nullable = false, length = 45, unique = true)
    private String ipAddress;

    /**
     * 封禁开始时间
     */
    @Column(nullable = false)
    private LocalDateTime blockStartTime;

    /**
     * 封禁结束时间
     */
    @Column(nullable = false)
    private LocalDateTime blockEndTime;

    /**
     * 封禁原因
     */
    @Column(length = 500)
    private String reason;

    /**
     * 是否是每日限制封禁
     */
    @Column(nullable = false)
    private Boolean isDailyLimit = false;

    public BlockedIp() {
    }

    public BlockedIp(String ipAddress, LocalDateTime blockEndTime, String reason, Boolean isDailyLimit) {
        this.ipAddress = ipAddress;
        this.blockStartTime = LocalDateTime.now();
        this.blockEndTime = blockEndTime;
        this.reason = reason;
        this.isDailyLimit = isDailyLimit;
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

    public LocalDateTime getBlockStartTime() {
        return blockStartTime;
    }

    public void setBlockStartTime(LocalDateTime blockStartTime) {
        this.blockStartTime = blockStartTime;
    }

    public LocalDateTime getBlockEndTime() {
        return blockEndTime;
    }

    public void setBlockEndTime(LocalDateTime blockEndTime) {
        this.blockEndTime = blockEndTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getIsDailyLimit() {
        return isDailyLimit;
    }

    public void setIsDailyLimit(Boolean dailyLimit) {
        isDailyLimit = dailyLimit;
    }

    /**
     * 检查IP是否仍在封禁中
     */
    public boolean isCurrentlyBlocked() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(blockEndTime);
    }
}
