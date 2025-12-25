package org.example.agent.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Agent通过此客户端连接到Socket服务，保持长连接
 * 接收Socket推送的命令并执行
 */
@Component
public class SocketClientEndpoint extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SocketClientEndpoint.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private WebSocketSession session;
    private String agentId;
    private String socketServerUrl;
    private volatile boolean connected = false;
    private ApplicationContext applicationContext;

    private CountDownLatch connectLatch = new CountDownLatch(1);

    /**
     * 设置应用上下文，用于关闭应用
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 异步连接到Socket服务，支持重试
     * @param agentId Agent标识
     * @param socketServerUrl Socket服务地址
     * @param maxRetries 最大重试次数
     * @param retryDelayMs 重试延迟(毫秒)
     */
    public void connectAsync(String agentId, String socketServerUrl, int maxRetries, long retryDelayMs) {
        new Thread(() -> {
            this.agentId = agentId;
            this.socketServerUrl = socketServerUrl;
            
            int attempt = 0;
            while (!connected && attempt < maxRetries) {
                try {
                    attemptConnect();
                    break;
                } catch (Exception e) {
                    attempt++;
                    if (attempt < maxRetries) {
                        log.warn("Failed to connect to Socket (attempt {}/{}), retrying in {}ms: {}", 
                                attempt, maxRetries, retryDelayMs, e.getMessage());
                        try {
                            Thread.sleep(retryDelayMs);
                        } catch (InterruptedException ie) {
                            log.error("Interrupted while waiting for retry", ie);
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        log.error("Failed to connect to Socket after {} attempts: {}", maxRetries, e.getMessage());
                    }
                }
            }
        }, "SocketConnectorThread").start();
    }

    /**
     * 同步连接到Socket服务，有超时控制
     */
    private void attemptConnect() throws Exception {
        String wsUrl = socketServerUrl.replace("http", "ws") + "/ws/agent/" + agentId;
        log.info("Attempting to connect to Socket service: {}", wsUrl);
        
        WebSocketClient client = new StandardWebSocketClient();
        this.session = client.doHandshake(this, wsUrl).get(5, TimeUnit.SECONDS);
        
        // 等待连接完成 (最多等待5秒)
        try {
            boolean success = connectLatch.await(5, TimeUnit.SECONDS);
            if (!success) {
                log.warn("WebSocket connection timeout");
                throw new Exception("Connection timeout");
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for WebSocket connection", e);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.session = session;
        this.connected = true;
        log.info("Connected to Socket service. Session: {}", session.getId());
        connectLatch.countDown();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.debug("Received message from Socket: {}", payload);
            
            // 解析消息
            var messageObj = objectMapper.readTree(payload);
            String type = messageObj.get("type") != null ? messageObj.get("type").asText() : "";
            
            switch (type) {
                case "command":
                    // 接收来自Socket的命令
                    handleCommand(messageObj);
                    break;
                case "ping":
                    // 响应心跳
                    sendHeartbeat();
                    break;
                default:
                    log.debug("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error handling message from Socket", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        this.connected = false;
        log.info("Disconnected from Socket service. Reason: {}", status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error", exception);
        this.connected = false;
    }

    /**
     * 处理来自Socket的命令
     */
    private void handleCommand(JsonNode messageObj) {
        try {
            // commandId可能不存在（向后兼容），使用-1作为默认值
            long commandId = -1;
            com.fasterxml.jackson.databind.JsonNode commandIdNode = messageObj.get("commandId");
            if (commandIdNode != null && !commandIdNode.isNull()) {
                commandId = commandIdNode.asLong();
            }
            
            String commandType = messageObj.get("commandType").asText();
            String commandContent = messageObj.get("commandContent").asText();
            
            log.info("Received command: id={}, type={}, content={}", commandId, commandType, commandContent);
            
            // 执行命令 (这里可以委托给命令执行器)
            boolean success = executeCommand(commandType, commandContent);
            
            // 如果有commandId，发送命令执行结果
            if (commandId > 0) {
                sendCommandResult(commandId, success, "Command executed successfully");
            } else {
                log.debug("No commandId provided, skipping result reporting");
            }
        } catch (Exception e) {
            log.error("Error handling command", e);
        }
    }

    /**
     * 执行命令 - 根据命令类型分配处理
     */
    private boolean executeCommand(String commandType, String commandContent) {
        log.info("Executing command: type={}, content={}", commandType, commandContent);
        
        try {
            switch (commandType.toLowerCase()) {
                case "offline":
                    return handleOfflineCommand();
                case "status":
                    return handleStatusCommand();
                case "restart":
                    return handleRestartCommand();
                default:
                    log.warn("Unknown command type: {}", commandType);
                    return false;
            }
        } catch (Exception e) {
            log.error("Error executing command: type={}", commandType, e);
            return false;
        }
    }

    /**
     * 处理强制下线命令 - 关闭Agent服务
     */
    private boolean handleOfflineCommand() {
        log.warn("Received offline command, agent will be shut down");
        
        try {
            // 立即发送命令结果确认
            sendCommandResult(0, true, "Agent is shutting down");
            
            // 延迟关闭，确保消息发送完成
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 等待1秒
                } catch (InterruptedException e) {
                    log.debug("Interrupted while waiting for shutdown", e);
                }
                
                // 关闭WebSocket连接
                close();
                
                // 关闭Spring应用
                if (applicationContext != null) {
                    log.info("Shutting down Agent application");
                    SpringApplication.exit(applicationContext, () -> 0);
                } else {
                    log.warn("ApplicationContext is null, cannot shutdown");
                    System.exit(0);
                }
            }, "AgentShutdownThread").start();
            
            return true;
        } catch (Exception e) {
            log.error("Error handling offline command", e);
            return false;
        }
    }

    /**
     * 处理状态查询命令
     */
    private boolean handleStatusCommand() {
        log.info("Status command received");
        // 可以返回Agent的状态信息
        return true;
    }

    /**
     * 处理重启命令
     */
    private boolean handleRestartCommand() {
        log.info("Restart command received");
        // 重启Agent应用
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.debug("Interrupted while waiting for restart", e);
            }
            
            close();
            
            if (applicationContext != null) {
                log.info("Restarting Agent application");
                SpringApplication.exit(applicationContext, () -> 1);
            }
        }, "AgentRestartThread").start();
        
        return true;
    }

    /**
     * 发送心跳消息给Socket
     */
    public void sendHeartbeat() {
        try {
            if (!isConnected()) {
                log.warn("Cannot send heartbeat: not connected to Socket");
                return;
            }
            
            Map<String, Object> heartbeat = new HashMap<>();
            heartbeat.put("type", "heartbeat");
            heartbeat.put("agentId", agentId);
            heartbeat.put("timestamp", System.currentTimeMillis());
            
            String messageJson = objectMapper.writeValueAsString(heartbeat);
            session.sendMessage(new TextMessage(messageJson));
            log.info("Heartbeat sent to Socket service (agentId: {})", agentId);
        } catch (IOException e) {
            log.error("Error sending heartbeat", e);
        }
    }

    /**
     * 发送命令执行结果
     */
    public void sendCommandResult(long commandId, boolean success, String result) {
        try {
            if (!isConnected()) {
                log.warn("Cannot send command result: not connected to Socket");
                return;
            }
            
            Map<String, Object> resultMessage = new HashMap<>();
            resultMessage.put("type", "command_result");
            resultMessage.put("commandId", commandId);
            resultMessage.put("success", success);
            resultMessage.put("result", result);
            resultMessage.put("timestamp", System.currentTimeMillis());
            
            String messageJson = objectMapper.writeValueAsString(resultMessage);
            session.sendMessage(new TextMessage(messageJson));
            log.info("Command result sent: id={}, success={}", commandId, success);
        } catch (IOException e) {
            log.error("Error sending command result", e);
        }
    }

    /**
     * 上报状态给Socket
     */
    public void reportStatus(Map<String, Object> statusData) {
        try {
            if (!isConnected()) {
                log.warn("Cannot report status: not connected to Socket");
                return;
            }
            
            Map<String, Object> statusMessage = new HashMap<>();
            statusMessage.put("type", "status");
            statusMessage.put("agentId", agentId);
            statusMessage.put("status", statusData);
            statusMessage.put("timestamp", System.currentTimeMillis());
            
            String messageJson = objectMapper.writeValueAsString(statusMessage);
            session.sendMessage(new TextMessage(messageJson));
            log.debug("Status reported to Socket");
        } catch (IOException e) {
            log.error("Error reporting status", e);
        }
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connected && session != null && session.isOpen();
    }

    /**
     * 获取Agent ID
     */
    public String getAgentId() {
        return agentId;
    }

    /**
     * 关闭连接
     */
    public void close() {
        try {
            if (session != null && session.isOpen()) {
                session.close(CloseStatus.NORMAL);
            }
        } catch (IOException e) {
            log.error("Error closing WebSocket connection", e);
        }
    }
}
