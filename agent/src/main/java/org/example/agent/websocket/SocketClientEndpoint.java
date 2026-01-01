package org.example.agent.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.agent.manager.ConfigManager;
import org.example.agent.manager.HeartbeatManager;
import org.example.agent.manager.RestartManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
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
    private final ConfigManager configManager;
    private final RestartManager restartManager;
    private HeartbeatManager heartbeatManager;

    private final CountDownLatch connectLatch = new CountDownLatch(1);

    public SocketClientEndpoint(ConfigManager configManager, RestartManager restartManager) {
        this.configManager = configManager;
        this.restartManager = restartManager;
    }

    /**
     * 设置HeartbeatManager，用于启动/停止心跳
     * 通过setter注入来避免循环依赖
     */
    public void setHeartbeatManager(HeartbeatManager heartbeatManager) {
        this.heartbeatManager = heartbeatManager;
    }

    /**
     * 设置应用上下文，用于关闭应用
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 异步连接到Socket服务，支持重试
     * @param agentId Agent 标识
     * @param socketServerUrl Socket 服务地址
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
        this.session = client.execute(this, wsUrl).get(5, TimeUnit.SECONDS);

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
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
        this.connected = true;
        log.info("Connected to Socket service. Session: {}", session.getId());
        
        // 连接成功后，启动心跳定时任务
        heartbeatManager.startHeartbeat();
        
        // 连接成功后，异步查询Server上保存的同步频率配置
        querySyncFrequencyAsync();
        
        connectLatch.countDown();
    }

    /**
     * 异步查询Server上保存的同步频率配置
     * 通过WebSocket发送查询请求给Socket服务
     */
    private void querySyncFrequencyAsync() {
        new Thread(() -> {
            try {
                Thread.sleep(500); // 延迟500ms，确保连接完全建立
                querySyncFrequency();
            } catch (InterruptedException e) {
                log.warn("Interrupted while querying sync frequency", e);
                Thread.currentThread().interrupt();
            }
        }, "SyncFrequencyQueryThread").start();
    }

    /**
     * 查询Server上保存的同步频率配置
     * 向Socket服务发送query_config请求
     */
    private void querySyncFrequency() {
        try {
            if (!isConnected()) {
                log.warn("Cannot query sync frequency: not connected to Socket");
                return;
            }
            
            Map<String, Object> queryMessage = new HashMap<>();
            queryMessage.put("type", "query_config");
            queryMessage.put("agentId", agentId);
            queryMessage.put("configType", "syncFrequency");
            queryMessage.put("timestamp", System.currentTimeMillis());
            
            String messageJson = objectMapper.writeValueAsString(queryMessage);
            session.sendMessage(new TextMessage(messageJson));
            log.info("Sync frequency query sent to Socket service (agentId: {})", agentId);
        } catch (Exception e) {
            log.error("Error querying sync frequency", e);
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        try {
            String payload = message.getPayload();
            log.debug("Received message from Socket: {}", payload);

            JsonNode messageObj = objectMapper.readTree(payload);
            String type = messageObj.get("type") != null ? messageObj.get("type").asText() : "";

            switch (type) {
                case "command":
                    handleCommand(messageObj);
                    break;
                case "config_response":
                    handleConfigResponse(messageObj);
                    break;
                case "ping":
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
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        this.connected = false;
        log.info("Disconnected from Socket service. Reason: {}", status);
        
        // 连接关闭时停止心跳
        heartbeatManager.stopHeartbeat();
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        log.error("WebSocket transport error", exception);
        this.connected = false;
    }

    /**
     * 处理来自Socket的配置响应
     * 包含Server查询返回的同步频率等配置信息
     */
    private void handleConfigResponse(JsonNode messageObj) {
        try {
            log.info("Received config response from Socket");
            
            // 提取syncFrequency
            if (messageObj.has("syncFrequency")) {
                Integer syncFrequencySeconds = messageObj.get("syncFrequency").asInt();
                
                if (syncFrequencySeconds != null && syncFrequencySeconds > 0) {
                    // 更新ConfigManager中的同步频率
                    configManager.updateSyncFrequency(syncFrequencySeconds);
                    log.info("Sync frequency initialized from Server: {}s", syncFrequencySeconds);
                } else {
                    log.warn("Invalid syncFrequency in config response: {}", syncFrequencySeconds);
                }
            } else {
                log.warn("syncFrequency field not found in config response");
            }
        } catch (Exception e) {
            log.error("Error handling config response", e);
        }
    }

    /**
     * 处理来自 Socket 的命令
     */
    private void handleCommand(JsonNode messageObj) {
        try {
            // commandId可能不存在（向后兼容），使用-1作为默认值
            long commandId = -1;
            JsonNode commandIdNode = messageObj.get("commandId");
            if (commandIdNode != null && !commandIdNode.isNull()) {
                commandId = commandIdNode.asLong();
            }

            String commandType = messageObj.get("commandType").asText();
            String commandContent = messageObj.get("commandContent").asText();

            log.info("Received command: id={}, type={}, content={}", commandId, commandType, commandContent);

            // 执行命令
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
            return switch (commandType.toLowerCase()) {
                case "offline" -> handleOfflineCommand();
                case "config" -> handleConfigCommand(commandContent);
                case "restart" -> handleRestartCommand();
                default -> {
                    log.warn("Unknown command type: {}", commandType);
                    yield false;
                }
            };
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
            // 在后台线程中执行关闭逻辑，不在这里发送消息
            // 让调用者（handleCommand）通过返回true后发送命令结果
            new Thread(() -> {
                try {
                    // 延迟2秒，确保命令结果消息已发送完成
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    log.debug("Interrupted while waiting for shutdown", e);
                    Thread.currentThread().interrupt();
                }
                
                // 关闭 WebSocket 连接
                close();
                
                // 关闭 Spring 应用
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
     * 处理配置更新命令
     * 解析Server发来的同步频率配置，更新ConfigManager
     * 
     * @param commandContent JSON格式的配置内容，例如: {"syncFrequency":60}
     * @return true表示配置更新成功，false表示更新失败
     */
    private boolean handleConfigCommand(String commandContent) {
        try {
            log.info("Processing config command: {}", commandContent);
            
            if (commandContent == null || commandContent.isEmpty()) {
                log.warn("Config command content is empty");
                return false;
            }
            
            // 解析JSON格式的配置内容
            JsonNode configNode = objectMapper.readTree(commandContent);
            
            // 提取同步频率（秒）
            if (configNode.has("syncFrequency")) {
                Integer syncFrequencySeconds = configNode.get("syncFrequency").asInt();
                
                if (syncFrequencySeconds != null && syncFrequencySeconds > 0) {
                    // 更新ConfigManager中的同步频率
                    configManager.updateSyncFrequency(syncFrequencySeconds);
                    log.info("Config updated successfully: syncFrequency={}s", syncFrequencySeconds);
                    return true;
                } else {
                    log.warn("Invalid syncFrequency value: {}", syncFrequencySeconds);
                    return false;
                }
            } else {
                log.warn("syncFrequency field not found in config command");
                return false;
            }
        } catch (Exception e) {
            log.error("Error processing config command: {}", commandContent, e);
            return false;
        }
    }

    /**
     * 处理重启命令
     */
    private boolean handleRestartCommand() {
        log.info("Restart command received, will restart Agent application");

        // 在后台线程中执行重启逻辑，不在这里发送消息
        // 让调用者（handleCommand）通过返回true后发送命令结果
        restartManager.restartAsync(applicationContext, 2000);
        
        return true;
    }

    /**
     * 发送心跳消息给 Socket
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
     * 上报状态给 Socket
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
     * 检查是否已连接到 Socket服务
     */
    public boolean isConnected() {
        return connected && session != null && session.isOpen();
    }

    /**
     * 关闭 WebSocket连接
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
