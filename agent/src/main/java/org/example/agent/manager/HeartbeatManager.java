package org.example.agent.manager;

import org.example.agent.websocket.SocketClientEndpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Agent心跳管理器
 * 定期发送心跳给Socket服务保持连接活跃
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
     */
    @Scheduled(fixedRateString = "${agent.heartbeat.interval}")
    public void sendHeartbeat() {
        try {
            if (socketClient.isConnected()) {
                socketClient.sendHeartbeat();
                log.info("Heartbeat sent successfully (interval: {}ms)", heartbeatInterval);
            } else {
                log.warn("Cannot send heartbeat: not connected to Socket service");
            }
        } catch (Exception e) {
            log.error("Error sending heartbeat", e);
        }
    }
}
