package org.example.socket.controller;

import org.example.socket.domain.PushMessage;
import org.example.socket.service.BroadcastService;
import org.example.socket.manager.AgentConnectionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/socket")
public class BroadcastController {

    private final BroadcastService broadcastService;
    private final AgentConnectionManager agentConnectionManager;
    private static final Logger log = LoggerFactory.getLogger(BroadcastController.class);

    public BroadcastController(BroadcastService broadcastService, AgentConnectionManager agentConnectionManager) {
        this.broadcastService = broadcastService;
        this.agentConnectionManager = agentConnectionManager;
    }

    /**
     * 推送设备状态变更消息
     */
    @PostMapping("/push/device-status")
    public ResponseEntity<String> pushDeviceStatus(@RequestBody PushMessage message) {
        broadcastService.broadcastDeviceStatusChange(
                message.getDeviceId(),
                message.getIpAddress(),
                message.getStatus(),
                message.getPayload()
        );
        return ResponseEntity.ok("Message pushed");
    }

    /**
     * 推送设备心跳消息
     */
    @PostMapping("/push/heartbeat")
    public ResponseEntity<String> pushHeartbeat(
            @RequestParam Long deviceId,
            @RequestParam String ipAddress) {
        broadcastService.broadcastHeartbeat(deviceId, ipAddress);
        return ResponseEntity.ok("Heartbeat pushed");
    }

    /**
     * 推送设备离线消息
     */
    @PostMapping("/push/device-offline")
    public ResponseEntity<String> pushDeviceOffline(
            @RequestParam Long deviceId,
            @RequestParam String ipAddress) {
        broadcastService.broadcastDeviceOffline(deviceId, ipAddress);
        return ResponseEntity.ok("Offline message pushed");
    }

    /**
     * 获取当前连接数
     */
    @GetMapping("/connections")
    public ResponseEntity<Map<String, Integer>> getConnectionCount() {
        Map<String, Integer> connections = new HashMap<>();
        connections.put("clients", broadcastService.getConnectionCount());
        connections.put("agents", agentConnectionManager.getAgentCount());
        return ResponseEntity.ok(connections);
    }

    /**
     * 向Agent转发命令 - Server调用此API向Agent发送命令
     * @param agentId 设备标识
     * @param command 命令内容
     */
    @PostMapping("/command/forward/{agentId}")
    public ResponseEntity<String> forwardCommandToAgent(
            @PathVariable String agentId,
            @RequestBody Map<String, Object> command) {
        try {
            if (!agentConnectionManager.isAgentOnline(agentId)) {
                return ResponseEntity.status(404).body("Agent not found or offline: " + agentId);
            }
            
            agentConnectionManager.sendCommandToAgent(agentId, command);
            return ResponseEntity.ok("Command forwarded to agent: " + agentId);
        } catch (Exception e) {
            log.error("Error forwarding command to agent: {}", agentId, e);
            return ResponseEntity.status(500).body("Failed to forward command: " + e.getMessage());
        }
    }

    /**
     * 检查指定Agent是否在线
     */
    @GetMapping("/agent/{agentId}/status")
    public ResponseEntity<Map<String, Object>> getAgentStatus(@PathVariable String agentId) {
        Map<String, Object> status = new HashMap<>();
        boolean isOnline = agentConnectionManager.isAgentOnline(agentId);
        status.put("agentId", agentId);
        status.put("online", isOnline);
        return ResponseEntity.ok(status);
    }

    /**
     * 获取所有在线Agent列表
     */
    @GetMapping("/agents/online")
    public ResponseEntity<Map<String, Object>> getOnlineAgents() {
        Map<String, Object> result = new HashMap<>();
        result.put("agents", agentConnectionManager.getOnlineAgentIds());
        result.put("count", agentConnectionManager.getAgentCount());
        return ResponseEntity.ok(result);
    }
}
