package org.example.socket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.socket.domain.PushMessage;
import org.example.socket.handler.DeviceMonitorWebSocketHandler;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BroadcastService {

    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(BroadcastService.class);

    public BroadcastService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 广播设备状态变更消息给所有前端客户端
     */
    public void broadcastDeviceStatusChange(Long deviceId, String ipAddress, String status, Object payload) {
        try {
            PushMessage message = new PushMessage("device_status_change", deviceId, ipAddress, status, payload);
            String jsonMessage = objectMapper.writeValueAsString(message);
            DeviceMonitorWebSocketHandler.broadcast(jsonMessage);
            log.info("Broadcasting device status change: {} - {}", ipAddress, status);
        } catch (Exception e) {
            log.error("Error broadcasting device status change", e);
        }
    }

    /**
     * 广播设备心跳消息给所有前端客户端
     */
    public void broadcastHeartbeat(Long deviceId, String ipAddress) {
        try {
            PushMessage message = new PushMessage("device_heartbeat", deviceId, ipAddress, "online", null);
            String jsonMessage = objectMapper.writeValueAsString(message);
            DeviceMonitorWebSocketHandler.broadcast(jsonMessage);
            log.debug("Broadcasting heartbeat from: {}", ipAddress);
        } catch (Exception e) {
            log.error("Error broadcasting heartbeat", e);
        }
    }

    /**
     * 广播设备离线消息给所有前端客户端
     */
    public void broadcastDeviceOffline(Long deviceId, String ipAddress) {
        try {
            PushMessage message = new PushMessage("device_offline", deviceId, ipAddress, "offline", null);
            String jsonMessage = objectMapper.writeValueAsString(message);
            DeviceMonitorWebSocketHandler.broadcast(jsonMessage);
            log.info("Broadcasting device offline: {}", ipAddress);
        } catch (Exception e) {
            log.error("Error broadcasting device offline", e);
        }
    }

    /**
     * 获取当前前端客户端连接数
     */
    public int getConnectionCount() {
        return DeviceMonitorWebSocketHandler.getClientConnectionCount();
    }
}
