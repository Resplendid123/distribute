package org.example.socket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.socket.manager.AgentConnectionManager;
import org.example.socket.service.DeviceManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Agent WebSocket处理器 - Spring WebSocket实现
 * 处理Agent连接、断开和消息接收
 */
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);
    private final AgentConnectionManager agentConnectionManager;
    private final ObjectMapper objectMapper;
    private final DeviceManagementService deviceManagementService;

    public AgentWebSocketHandler(AgentConnectionManager agentConnectionManager,
                                 ObjectMapper objectMapper,
                                 DeviceManagementService deviceManagementService) {
        this.agentConnectionManager = agentConnectionManager;
        this.objectMapper = objectMapper;
        this.deviceManagementService = deviceManagementService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            // 从WebSocket URI路径中提取agentId，格式: /ws/agent/{agentId}
            String agentId = extractDeviceId(session);

            String clientIpPort = (String) session.getAttributes().get("clientIpPort");
            if (clientIpPort == null || clientIpPort.isEmpty()) {
                clientIpPort = "unknown";
                log.warn("Client IP:PORT not found in session properties for agent: {}", agentId);
            }

            // 在AgentConnectionManager中注册Agent（使用agentId作为key）
            agentConnectionManager.registerAgent(agentId, session);
            log.info("Agent connected: {} (IP:PORT: {}, SessionId: {})", agentId, clientIpPort, session.getId());
            
            // 通过agentId查询或创建设备记录
            // agentId对应数据库中Device.name字段，deviceManagementService会通过name查询并返回设备ID
            deviceManagementService.registerOrUpdateDevice(agentId, clientIpPort);
            log.info("Device registered/updated in database for agent: {} with IP:PORT: {}", agentId, clientIpPort);
        } catch (Exception e) {
            log.error("Error during agent connection", e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String deviceId = extractDeviceId(session);
            String payload = message.getPayload();
            log.debug("Received message from device {}: {}", deviceId, payload);
            
            // 解析消息类型
            var messageObj = objectMapper.readTree(payload);
            String type = messageObj.get("type") != null ? messageObj.get("type").asText() : "";
            
            switch (type) {
                case "heartbeat":
                    // 心跳消息 - 更新最后心跳时间
                    deviceManagementService.updateDeviceHeartbeat(deviceId);
                    log.debug("Heartbeat received from device: {} (timestamp: {})", deviceId, System.currentTimeMillis());
                    break;
                case "status":
                    // 状态更新 - 广播给前端
                    DeviceMonitorWebSocketHandler.broadcast(payload);
                    deviceManagementService.updateDeviceStatus(deviceId, messageObj.get("status"));
                    log.debug("Status update from device: {}", deviceId);
                    break;
                case "command_result":
                    // 命令执行结果 - 广播给前端
                    DeviceMonitorWebSocketHandler.broadcast(payload);
                    log.debug("Command result from device: {}", deviceId);
                    break;
                default:
                    log.debug("Unknown message type from device {}: {}", deviceId, type);
            }
        } catch (Exception e) {
            log.error("Error handling message from agent", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            String deviceId = extractDeviceId(session);
            agentConnectionManager.unregisterAgent(deviceId);
            log.info("Device disconnected: {} (CloseStatus: {})", deviceId, status);
            
            // Device断开连接时，标记为离线
            deviceManagementService.markDeviceOffline(deviceId);
            log.info("Device marked offline in database: {}", deviceId);
        } catch (Exception e) {
            log.error("Error during agent disconnection", e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        try {
            String deviceId = extractDeviceId(session);
            log.error("WebSocket transport error in device {}", deviceId, exception);
        } catch (Exception e) {
            log.error("Error handling transport error", e);
        }
    }

    /**
     * 从session中提取Device ID
     */
    private String extractDeviceId(WebSocketSession session) {
        Object deviceIdObj = session.getAttributes().get("deviceId");
        if (deviceIdObj != null) {
            return deviceIdObj.toString();
        }
        String path = session.getUri().getPath();
        if (path.contains("/ws/agent/")) {
            return path.substring(path.lastIndexOf("/") + 1);
        }
        return "unknown";
    }
}
