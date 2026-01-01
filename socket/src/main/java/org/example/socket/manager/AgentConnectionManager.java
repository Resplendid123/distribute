package org.example.socket.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent连接管理器 - 维护所有连接的Agent会话
 * 支持按ID查找Agent并推送命令
 */
@Component
public class AgentConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(AgentConnectionManager.class);

    private static final Map<String, AgentSession> agentSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    public AgentConnectionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 注册Agent连接
     */
    public void registerAgent(String name, WebSocketSession session) {
        try {
            agentSessions.put(name, new AgentSession(name, session));
            log.info("Agent registered: {}, Total agents: {}", name, agentSessions.size());
        } catch (Exception e) {
            log.error("Error registering agent: {}", name, e);
        }
    }

    /**
     * 注销Agent连接
     */
    public void unregisterAgent(String name) {
        agentSessions.remove(name);
        log.info("Agent unregistered: {}, Remaining agents: {}", name, agentSessions.size());
    }

    /**
     * 向指定Agent发送命令
     */
    public void sendCommandToAgent(String name, Map<String, Object> command) {
        try {
            AgentSession agentSession = agentSessions.get(name);
            if (agentSession == null) {
                log.warn("Agent not found: {}", name);
                return;
            }
            

            if (!agentSession.isConnected()) {
                log.warn("Agent not connected: {}", name);
                agentSessions.remove(name);
                return;
            }
String messageJson = objectMapper.writeValueAsString(command);
            agentSession.sendMessage(messageJson);
            log.info("Command sent to agent: {}", name);
        } catch (Exception e) {
            log.error("Error sending command to agent: {}", name, e);
        }
    }

    /**
     * 获取Agent连接数
     */
    public int getAgentCount() {
        return agentSessions.size();
    }

    /**
     * 获取所有在线Agent ID
     */
    public java.util.Set<String> getOnlineAgentIds() {
        return agentSessions.keySet();
    }

    /**
     * 检查Agent是否在线
     */
    public boolean isAgentOnline(String name) {
        AgentSession session = agentSessions.get(name);
        return session != null && session.isConnected();
    }

    /**
     * 内部类：Agent会话包装 - Spring WebSocket实现
     */
    public static class AgentSession {
        private final String name;
        private final WebSocketSession session;
        private final long createdAt;

        public AgentSession(String name, WebSocketSession session) {
            this.name = name;
            this.session = session;
            this.createdAt = System.currentTimeMillis();
        }

        public String getAgentId() {
            return name;
        }

        public WebSocketSession getSession() {
            return session;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public boolean isConnected() {
            return session != null && session.isOpen();
        }

        public void sendMessage(String message) throws IOException {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }
}
