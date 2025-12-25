package org.example.socket.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 设备监控 WebSocket处理器 - Spring WebSocket实现
 * 支持前端客户端连接 (/ws/monitor) - 用于接收状态广播
 */
public class DeviceMonitorWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceMonitorWebSocketHandler.class);
    private static final Set<WebSocketSession> clientSessions = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        clientSessions.add(session);
        log.info("Client connected to /ws/monitor: {}, Total clients: {}", session.getId(), clientSessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Received message from {}: {}", session.getId(), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        clientSessions.remove(session);
        log.info("Client disconnected from /ws/monitor: {} (CloseStatus: {}), Total clients: {}", 
                 session.getId(), status, clientSessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error in {}", session.getId(), exception);
    }

    /**
     * 广播消息到所有连接的前端客户端
     */
    public static void broadcast(String message) {
        for (WebSocketSession session : clientSessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.warn("Failed to send message to {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * 获取当前前端客户端连接数
     */
    public static int getClientConnectionCount() {
        return clientSessions.size();
    }
}
