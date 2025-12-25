package org.example.server.domain.vo;

import java.time.LocalDateTime;

public class DeviceVo {
    private Long id;
    private String name;
    private String ipAddress;
    private Integer syncFrequency;
    private Integer statusCode;
    private String info;
    private LocalDateTime lastHeartbeatAt;
    private Boolean isOnline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public DeviceVo() {
    }

    public DeviceVo(Long id, String name, String ipAddress, Integer syncFrequency, Integer statusCode, String info, LocalDateTime lastHeartbeatAt, Boolean isOnline, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
        this.syncFrequency = syncFrequency;
        this.statusCode = statusCode;
        this.info = info;
        this.lastHeartbeatAt = lastHeartbeatAt;
        this.isOnline = isOnline;
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

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
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

    // Builder pattern support
    public static class Builder {
        private Long id;
        private String name;
        private String ipAddress;
        private Integer syncFrequency;
        private Integer statusCode;
        private String info;
        private LocalDateTime lastHeartbeatAt;
        private Boolean isOnline;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public Builder syncFrequency(Integer syncFrequency) { this.syncFrequency = syncFrequency; return this; }
        public Builder statusCode(Integer statusCode) { this.statusCode = statusCode; return this; }
        public Builder info(String info) { this.info = info; return this; }
        public Builder lastHeartbeatAt(LocalDateTime lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; return this; }
        public Builder isOnline(Boolean isOnline) { this.isOnline = isOnline; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public DeviceVo build() {
            return new DeviceVo(id, name, ipAddress, syncFrequency, statusCode, info, lastHeartbeatAt, isOnline, createdAt, updatedAt);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
