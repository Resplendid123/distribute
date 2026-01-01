package org.example.agent.manager;

import org.example.agent.websocket.SocketClientEndpoint;
import org.example.agent.util.SystemInfoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Agent心跳管理器
 * 定期发送心跳给Socket服务保持连接活跃
 * 同时上报系统状态信息（CPU、内存、磁盘等）
 * 支持动态修改心跳间隔，当ConfigManager中的同步频率更新时，
 * 心跳间隔会自动同步更新
 */
@Component
public class HeartbeatManager {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatManager.class);

    private SocketClientEndpoint socketClient;
    private final ConfigManager configManager;

    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> currentTask;
    private volatile long currentInterval;

    public HeartbeatManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * 设置SocketClientEndpoint，用于发送心跳
     * 通过setter注入来避免循环依赖
     */
    public void setSocketClientEndpoint(SocketClientEndpoint socketClient) {
        this.socketClient = socketClient;
    }

    /**
     * 初始化心跳定时任务
     * 应用启动后调用
     */
    @PostConstruct
    public void init() {
        executorService = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "HeartbeatThread");
            t.setDaemon(false);
            return t;
        });
        
        // 使用 ConfigManager 中的同步频率作为心跳间隔
        currentInterval = configManager.getHeartbeatIntervalMs();
        
        // 不立即启动心跳，而是等待连接建立后再启动
        // 这样可以确保首次心跳时已经连接到Socket服务
        log.info("HeartbeatManager initialized, waiting for Socket connection before starting heartbeat (interval: {}ms)", currentInterval);
    }

    /**
     * 启动心跳定时任务
     */
    private void scheduleHeartbeat() {
        if (currentTask != null) {
            currentTask.cancel(false);
        }
        
        currentTask = executorService.scheduleAtFixedRate(
            this::sendHeartbeat,
            currentInterval,
            currentInterval,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * 定期发送心跳
     * 同时上报系统状态信息（CPU、内存、磁盘等）
     */
    private void sendHeartbeat() {
        try {
            // 检查同步频率是否已更新，如果更新则重新调度
            long newInterval = configManager.getHeartbeatIntervalMs();
            if (newInterval != currentInterval) {
                log.info("Heartbeat interval changed: {} -> {}ms, rescheduling task", currentInterval, newInterval);
                currentInterval = newInterval;
                scheduleHeartbeat();
                return; // 跳过本次心跳，等待下一个周期
            }
            
            if (socketClient.isConnected()) {
                // 获取系统信息
                Map<String, Object> systemInfo = SystemInfoUtil.getSystemInfo();
                
                // 上报状态给 Socket服务
                socketClient.reportStatus(systemInfo);
                
                log.info("Heartbeat sent successfully with system info (interval: {}ms)", currentInterval);
                log.debug("System info - Disk: {}, Memory: {}, CPU: {}", 
                    systemInfo.get("disk"), 
                    systemInfo.get("memory"), 
                    systemInfo.get("cpu"));
            } else {
                log.warn("Cannot send heartbeat: not connected to Socket service");
            }
        } catch (Exception e) {
            log.error("Error sending heartbeat", e);
        }
    }

    /**
     * 销毁资源，关闭心跳线程池
     */
    @PreDestroy
    public void destroy() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("HeartbeatManager destroyed");
    }

    /**
     * 启动心跳定时任务
     * 在Socket连接建立后由SocketClientEndpoint调用
     */
    public void startHeartbeat() {
        if (currentTask != null) {
            log.warn("Heartbeat already started");
            return;
        }
        
        scheduleHeartbeat();
        log.info("Heartbeat started with interval: {}ms", currentInterval);
    }

    /**
     * 停止心跳定时任务
     */
    public void stopHeartbeat() {
        if (currentTask != null) {
            currentTask.cancel(false);
            currentTask = null;
            log.info("Heartbeat stopped");
        }
    }
}