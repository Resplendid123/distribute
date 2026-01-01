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

import java.util.HashMap;
import java.util.Map;

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
            // 从WebSocket URI路径中提取name，格式: /ws/agent/{name}
            String name = extractDeviceId(session);

            String clientIpPort = (String) session.getAttributes().get("clientIpPort");
            if (clientIpPort == null || clientIpPort.isEmpty()) {
                clientIpPort = "unknown";
                log.warn("Client IP:PORT not found in session properties for agent: {}", name);
            }

            // 在AgentConnectionManager中注册Agent（使用name作为key）
            agentConnectionManager.registerAgent(name, session);
            log.info("Agent connected: {} (IP:PORT: {}, SessionId: {})", name, clientIpPort, session.getId());
            
            // 创建新的设备记录，使用Agent name
            // 返回创建的设备ID，保存到session中供后续使用
            Long deviceId = deviceManagementService.registerOrUpdateDevice(name, clientIpPort);
            if (deviceId != null) {
                session.getAttributes().put("deviceId", deviceId);
                log.info("Device created in database for agent: {} with id={}, IP:PORT: {}", name, deviceId, clientIpPort);
            } else {
                log.error("Failed to create device record for agent: {}", name);
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception e) {
            log.error("Error during agent connection", e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Object deviceIdObj = session.getAttributes().get("deviceId");
            if (!(deviceIdObj instanceof Long)) {
                log.warn("deviceId not found or invalid in session");
                return;
            }
            Long deviceId = (Long) deviceIdObj;
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
                case "query_config":
                    // 配置查询 - 返回该设备的同步频率配置
                    handleConfigQuery(session, deviceId);
                    log.debug("Config query from device: {}", deviceId);
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
            String agentName = extractDeviceId(session);
            agentConnectionManager.unregisterAgent(agentName);
            log.info("Agent disconnected: {} (CloseStatus: {})", agentName, status);
            
            // 标记设备离线 - 使用session中保存的deviceId
            Object deviceIdObj = session.getAttributes().get("deviceId");
            if (deviceIdObj instanceof Long) {
                Long deviceId = (Long) deviceIdObj;
                deviceManagementService.markDeviceOffline(deviceId);
                log.info("Device marked offline in database: {}", deviceId);
            } else {
                log.warn("deviceId not found in session for offline marking");
            }
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
     * 处理来自Agent的配置查询请求
     * Agent在连接成功后会查询Server保存的同步频率配置
     */
    private void handleConfigQuery(WebSocketSession session, Long deviceId) {
        try {
            // 从数据库查询该设备的配置信息
            org.example.socket.domain.Device device = deviceManagementService.getDeviceById(deviceId);
            
            if (device == null) {
                log.warn("Device not found for config query: {}", deviceId);
                return;
            }
            
            // 构建配置响应消息
            Map<String, Object> configResponse = new HashMap<>();
            configResponse.put("type", "config_response");
            configResponse.put("deviceId", deviceId);
            configResponse.put("syncFrequency", device.getSyncFrequency());
            configResponse.put("timestamp", System.currentTimeMillis());
            
            String responseJson = objectMapper.writeValueAsString(configResponse);
            session.sendMessage(new TextMessage(responseJson));
            
            log.info("Config sent to device {}: syncFrequency={}s", deviceId, device.getSyncFrequency());
        } catch (Exception e) {
            log.error("Error handling config query for device: {}", deviceId, e);
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
