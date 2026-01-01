package org.example.agent;

import org.example.agent.manager.HeartbeatManager;
import org.example.agent.manager.RestartManager;
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
    private final RestartManager restartManager;
    private final HeartbeatManager heartbeatManager;

    @Value("${agent.name}")
    private String name;

    @Value("${socket.server.url}")
    private String socketServerUrl;

    @Value("${socket.connect.max-retries}")
    private int maxRetries;

    @Value("${socket.connect.retry-delay-ms}")
    private long retryDelayMs;

    public AgentApplication(SocketClientEndpoint socketClient, ApplicationContext applicationContext, RestartManager restartManager, HeartbeatManager heartbeatManager) {
        this.socketClient = socketClient;
        this.applicationContext = applicationContext;
        this.restartManager = restartManager;
        this.heartbeatManager = heartbeatManager;
    }

    public static void main(String[] args) {
        // 保存启动参数到系统属性，用于后续重启时使用
        RestartManager.saveBootArgs(args);
        log.info("Agent startup with args: {}", String.join(" ", args));

        SpringApplication.run(AgentApplication.class, args);
    }

    /**
     * 应用启动完成后，异步连接到Socket服务
     * 不会阻塞应用启动
     */
    @EventListener(ApplicationReadyEvent.class)
    public void connectToSocketServer() {
        try {
            log.info("Agent starting up: id={}, socket-server={}", name, socketServerUrl);
            log.info("Attempting to connect to Socket service (max retries: {}, retry delay: {}ms)",
                    maxRetries, retryDelayMs);

            // 设置应用上下文，用于处理关闭命令
            socketClient.setApplicationContext(applicationContext);
            
            // 完成SocketClientEndpoint和HeartbeatManager的相互关联（避免循环依赖）
            socketClient.setHeartbeatManager(heartbeatManager);
            heartbeatManager.setSocketClientEndpoint(socketClient);
            
            // 设置SocketClientEndpoint到RestartManager，用于关闭WebSocket连接
            restartManager.setSocketClientEndpoint(socketClient);

            socketClient.connectAsync(name, socketServerUrl, maxRetries, retryDelayMs);

            log.info("Agent started successfully. Socket connection attempt started in background.");
        } catch (Exception e) {
            log.error("Error during agent startup", e);
        }
    }
}

