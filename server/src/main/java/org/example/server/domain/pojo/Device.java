package org.example.server.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("devices")
public class Device {
    private Long id;
    private String name;
    private String remarkName;
    private String ipAddress;
    private Integer syncFrequency;
    private Integer statusCode;
    private String info;
    private LocalDateTime lastHeartbeatAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Device() {
    }

    public Device(Long id, String name, String remarkName, String ipAddress, Integer syncFrequency, Integer statusCode, String info, LocalDateTime lastHeartbeatAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.remarkName = remarkName;
        this.ipAddress = ipAddress;
        this.syncFrequency = syncFrequency;
        this.statusCode = statusCode;
        this.info = info;
        this.lastHeartbeatAt = lastHeartbeatAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemarkName() {
        return remarkName;
    }

    public void setRemarkName(String remarkName) {
        this.remarkName = remarkName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getSyncFrequency() {
        return syncFrequency;
    }

    public void setSyncFrequency(Integer syncFrequency) {
        this.syncFrequency = syncFrequency;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public LocalDateTime getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
