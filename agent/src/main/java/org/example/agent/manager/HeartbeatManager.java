package org.example.agent.manager;

import org.example.agent.websocket.SocketClientEndpoint;
import org.example.agent.util.SystemInfoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Agent心跳管理器
 * 定期发送心跳给Socket服务保持连接活跃
 * 同时上报系统状态信息（CPU、内存、磁盘等）
 */
@Component
public class HeartbeatManager {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatManager.class);
    
    private final SocketClientEndpoint socketClient;
    
    @Value("${agent.heartbeat.interval}")
    private long heartbeatInterval;

    public HeartbeatManager(SocketClientEndpoint socketClient) {
        this.socketClient = socketClient;
    }

    /**
     * 定期发送心跳
     * 间隔时间由配置 agent.heartbeat.interval 决定，默认30秒
     * 同时上报系统状态信息（CPU、内存、磁盘等）
     */
    @Scheduled(fixedRateString = "${agent.heartbeat.interval}")
    public void sendHeartbeat() {
        try {
            if (socketClient.isConnected()) {
                // 获取系统信息
                Map<String, Object> systemInfo = SystemInfoUtil.getSystemInfo();
                
                // 上报状态给 Socket服务
                socketClient.reportStatus(systemInfo);
                
                log.info("Heartbeat sent successfully with system info (interval: {}ms)", heartbeatInterval);
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
}
