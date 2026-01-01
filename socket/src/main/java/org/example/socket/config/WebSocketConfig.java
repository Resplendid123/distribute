package org.example.socket.config;

import org.example.socket.handler.AgentWebSocketHandler;
import org.example.socket.handler.DeviceMonitorWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AgentWebSocketHandler agentWebSocketHandler;
    private final DeviceMonitorWebSocketHandler deviceMonitorWebSocketHandler;

    public WebSocketConfig(AgentWebSocketHandler agentWebSocketHandler,
                          DeviceMonitorWebSocketHandler deviceMonitorWebSocketHandler) {
        this.agentWebSocketHandler = agentWebSocketHandler;
        this.deviceMonitorWebSocketHandler = deviceMonitorWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(agentWebSocketHandler, "/ws/agent/{deviceId}")
                .addInterceptors(new AgentWebSocketHandshakeInterceptor())
                .setAllowedOrigins("*");

        registry.addHandler(deviceMonitorWebSocketHandler, "/ws/monitor")
                .setAllowedOrigins("*");
    }
}