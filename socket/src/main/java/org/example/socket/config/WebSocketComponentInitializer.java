package org.example.socket.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.socket.handler.AgentWebSocketHandler;
import org.example.socket.handler.DeviceMonitorWebSocketHandler;
import org.example.socket.manager.AgentConnectionManager;
import org.example.socket.service.DeviceManagementService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket组件初始化配置
 * 负责注入WebSocket处理器所需的依赖
 */
@Configuration
public class WebSocketComponentInitializer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketComponentInitializer.class);

    @Bean
    public AgentWebSocketHandler agentWebSocketHandler(AgentConnectionManager agentConnectionManager,
                                                       ObjectMapper objectMapper,
                                                       DeviceManagementService deviceManagementService) {
        return new AgentWebSocketHandler(agentConnectionManager, objectMapper, deviceManagementService);
    }

    @Bean
    public DeviceMonitorWebSocketHandler deviceMonitorWebSocketHandler() {
        return new DeviceMonitorWebSocketHandler();
    }

    @Bean
    public ApplicationRunner initializeWebSocketComponents() {
        return args -> {
            log.info("WebSocket components initialized with Spring WebSocket");
        };
    }
}

