package org.example.agent;

import org.example.agent.websocket.SocketClientEndpoint;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgentApplication {

    private static final Logger log = LoggerFactory.getLogger(AgentApplication.class);
    
    private final SocketClientEndpoint socketClient;
    private final ApplicationContext applicationContext;
    
    @Value("${agent.id}")
    private String agentId;
    
    @Value("${socket.server.url}")
    private String socketServerUrl;
    
    @Value("${socket.connect.max-retries}")
    private int maxRetries;
    
    @Value("${socket.connect.retry-delay-ms}")
    private long retryDelayMs;

    public AgentApplication(SocketClientEndpoint socketClient, ApplicationContext applicationContext) {
        this.socketClient = socketClient;
        this.applicationContext = applicationContext;
    }

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }

    /**
     * 应用启动完成后，异步连接到Socket服务
     * 不会阻塞应用启动
     */
    @EventListener(ApplicationReadyEvent.class)
    public void connectToSocketServer() {
        try {
            log.info("Agent starting up: id={}, socket-server={}", agentId, socketServerUrl);
            log.info("Attempting to connect to Socket service (max retries: {}, retry delay: {}ms)", 
                    maxRetries, retryDelayMs);

            // 设置应用上下文，用于处理关闭命令
            socketClient.setApplicationContext(applicationContext);
            
            socketClient.connectAsync(agentId, socketServerUrl, maxRetries, retryDelayMs);
            
            log.info("Agent started successfully. Socket connection attempt started in background.");
        } catch (Exception e) {
            log.error("Error during agent startup", e);
        }
    }
}

