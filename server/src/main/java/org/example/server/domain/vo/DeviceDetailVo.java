package org.example.server.domain.vo;

import org.example.server.util.SystemInfoParseUtil;
import java.time.LocalDateTime;
import java.util.Map;

public class DeviceDetailVo {
    private Long id;
    private String name;
    private String ipAddress;
    private Integer syncFrequency;
    private Integer statusCode;
    private String info;
    private LocalDateTime lastHeartbeatAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 系统信息字段
    private String diskTotal;
    private String diskUsed;
    private String diskUsagePercentage;
    private String heapMemoryUsed;
    private String heapMemoryMax;
    private String heapMemoryUsagePercentage;
    private String systemMemoryTotal;
    private String systemMemoryUsed;
    private String systemMemoryUsagePercentage;
    private String processCpuUsage;
    private String systemCpuUsage;
    private Integer cpuProcessorCount;

    public DeviceDetailVo() {
    }

    public DeviceDetailVo(Long id, String name, String ipAddress, Integer syncFrequency, Integer statusCode, String info, LocalDateTime lastHeartbeatAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
        this.syncFrequency = syncFrequency;
        this.statusCode = statusCode;
        this.info = info;
        this.lastHeartbeatAt = lastHeartbeatAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        parseSystemInfo();
    }

    /**
     * 从 JSON信息中解析系统信息
     */
    private void parseSystemInfo() {
        if (info == null || info.isEmpty()) {
            return;
        }
        
        try {
            Map<String, Object> parsed = SystemInfoParseUtil.parseSystemInfo(info);

            if (parsed.containsKey("disk")) {
                @SuppressWarnings("unchecked")
                Map<String, String> diskInfo = (Map<String, String>) parsed.get("disk");
                this.diskTotal = diskInfo.get("total");
                this.diskUsed = diskInfo.get("used");
                this.diskUsagePercentage = diskInfo.get("usagePercentage");
            }

            if (parsed.containsKey("memory")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> memoryInfo = (Map<String, Object>) parsed.get("memory");
                
                if (memoryInfo.containsKey("heap")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> heapInfo = (Map<String, String>) memoryInfo.get("heap");
                    this.heapMemoryUsed = heapInfo.get("used");
                    this.heapMemoryMax = heapInfo.get("max");
                    this.heapMemoryUsagePercentage = heapInfo.get("usagePercentage");
                }
                
                if (memoryInfo.containsKey("system")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> systemInfo = (Map<String, String>) memoryInfo.get("system");
                    this.systemMemoryTotal = systemInfo.get("total");
                    this.systemMemoryUsed = systemInfo.get("used");
                    this.systemMemoryUsagePercentage = systemInfo.get("usagePercentage");
                }
            }

            if (parsed.containsKey("cpu")) {
                @SuppressWarnings("unchecked")
                Map<String, String> cpuInfo = (Map<String, String>) parsed.get("cpu");
                this.processCpuUsage = cpuInfo.get("processCpuUsage");
                this.systemCpuUsage = cpuInfo.get("systemCpuUsage");
                String processorCount = cpuInfo.get("availableProcessors");
                if (processorCount != null) {
                    try {
                        this.cpuProcessorCount = Integer.parseInt(processorCount);
                    } catch (NumberFormatException e) {
                        this.cpuProcessorCount = null;
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败，保持字段为null
        }
    }

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

    public String getDiskTotal() {
        return diskTotal;
    }

    public void setDiskTotal(String diskTotal) {
        this.diskTotal = diskTotal;
    }

    public String getDiskUsed() {
        return diskUsed;
    }

    public void setDiskUsed(String diskUsed) {
        this.diskUsed = diskUsed;
    }

    public String getDiskUsagePercentage() {
        return diskUsagePercentage;
    }

    public void setDiskUsagePercentage(String diskUsagePercentage) {
        this.diskUsagePercentage = diskUsagePercentage;
    }

    public String getHeapMemoryUsed() {
        return heapMemoryUsed;
    }

    public void setHeapMemoryUsed(String heapMemoryUsed) {
        this.heapMemoryUsed = heapMemoryUsed;
    }

    public String getHeapMemoryMax() {
        return heapMemoryMax;
    }

    public void setHeapMemoryMax(String heapMemoryMax) {
        this.heapMemoryMax = heapMemoryMax;
    }

    public String getHeapMemoryUsagePercentage() {
        return heapMemoryUsagePercentage;
    }

    public void setHeapMemoryUsagePercentage(String heapMemoryUsagePercentage) {
        this.heapMemoryUsagePercentage = heapMemoryUsagePercentage;
    }

    public String getSystemMemoryTotal() {
        return systemMemoryTotal;
    }

    public void setSystemMemoryTotal(String systemMemoryTotal) {
        this.systemMemoryTotal = systemMemoryTotal;
    }

    public String getSystemMemoryUsed() {
        return systemMemoryUsed;
    }

    public void setSystemMemoryUsed(String systemMemoryUsed) {
        this.systemMemoryUsed = systemMemoryUsed;
    }

    public String getSystemMemoryUsagePercentage() {
        return systemMemoryUsagePercentage;
    }

    public void setSystemMemoryUsagePercentage(String systemMemoryUsagePercentage) {
        this.systemMemoryUsagePercentage = systemMemoryUsagePercentage;
    }

    public String getProcessCpuUsage() {
        return processCpuUsage;
    }

    public void setProcessCpuUsage(String processCpuUsage) {
        this.processCpuUsage = processCpuUsage;
    }

    public String getSystemCpuUsage() {
        return systemCpuUsage;
    }

    public void setSystemCpuUsage(String systemCpuUsage) {
        this.systemCpuUsage = systemCpuUsage;
    }

    public Integer getCpuProcessorCount() {
        return cpuProcessorCount;
    }

    public void setCpuProcessorCount(Integer cpuProcessorCount) {
        this.cpuProcessorCount = cpuProcessorCount;
    }

    public static class Builder {
        private Long id;
        private String name;
        private String ipAddress;
        private Integer syncFrequency;
        private Integer statusCode;
        private String info;
        private LocalDateTime lastHeartbeatAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public Builder syncFrequency(Integer syncFrequency) { this.syncFrequency = syncFrequency; return this; }
        public Builder statusCode(Integer statusCode) { this.statusCode = statusCode; return this; }
        public Builder info(String info) { this.info = info; return this; }
        public Builder lastHeartbeatAt(LocalDateTime lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public DeviceDetailVo build() {
            return new DeviceDetailVo(id, name, ipAddress, syncFrequency, statusCode, info, lastHeartbeatAt, createdAt, updatedAt);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
