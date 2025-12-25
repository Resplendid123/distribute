package org.example.socket.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

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
    public void registerAgent(String agentId, WebSocketSession session) {
        try {
            agentSessions.put(agentId, new AgentSession(agentId, session));
            log.info("Agent registered: {}, Total agents: {}", agentId, agentSessions.size());
        } catch (Exception e) {
            log.error("Error registering agent: {}", agentId, e);
        }
    }

    /**
     * 注销Agent连接
     */
    public void unregisterAgent(String agentId) {
        agentSessions.remove(agentId);
        log.info("Agent unregistered: {}, Remaining agents: {}", agentId, agentSessions.size());
    }

    /**
     * 向指定Agent发送命令
     */
    public void sendCommandToAgent(String agentId, Map<String, Object> command) {
        try {
            AgentSession agentSession = agentSessions.get(agentId);
            if (agentSession == null) {
                log.warn("Agent not found: {}", agentId);
                return;
            }
            
            if (!agentSession.isConnected()) {
                log.warn("Agent not connected: {}", agentId);
                agentSessions.remove(agentId);
                return;
            }
            
            String messageJson = objectMapper.writeValueAsString(command);
            agentSession.sendMessage(messageJson);
            log.info("Command sent to agent: {}", agentId);
        } catch (Exception e) {
            log.error("Error sending command to agent: {}", agentId, e);
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
    public boolean isAgentOnline(String agentId) {
        AgentSession session = agentSessions.get(agentId);
        return session != null && session.isConnected();
    }

    /**
     * 内部类：Agent会话包装 - Spring WebSocket实现
     */
    public static class AgentSession {
        private final String agentId;
        private final WebSocketSession session;
        private final long createdAt;

        public AgentSession(String agentId, WebSocketSession session) {
            this.agentId = agentId;
            this.session = session;
            this.createdAt = System.currentTimeMillis();
        }

        public String getAgentId() {
            return agentId;
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
