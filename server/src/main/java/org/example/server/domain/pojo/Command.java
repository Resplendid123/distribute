package org.example.server.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("commands")
public class Command {
    private Long id;
    private Long deviceId;
    private String commandType;
    private String commandContent;
    private String status;
    private String result;
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Command() {
    }

    public Command(Long id, Long deviceId, String commandType, String commandContent, String status, String result, LocalDateTime createdAt, LocalDateTime executedAt, LocalDateTime updatedAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.commandType = commandType;
        this.commandContent = commandContent;
        this.status = status;
        this.result = result;
        this.createdAt = createdAt;
        this.executedAt = executedAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public String getCommandContent() {
        return commandContent;
    }

    public void setCommandContent(String commandContent) {
        this.commandContent = commandContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
