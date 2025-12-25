package org.example.socket.domain;

public class PushMessage {
    private String type;
    private Long deviceId;
    private String ipAddress;
    private String status;
    private Object payload;
    private Long timestamp;

    // Constructors
    public PushMessage() {
    }

    public PushMessage(String type, Long deviceId, String ipAddress, String status, Object payload) {
        this.type = type;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.status = status;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
