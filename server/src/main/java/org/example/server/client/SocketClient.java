package org.example.server.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Socket服务客户端
 * Server通过此客户端与Socket服务通信
 * 用于向Socket转发命令，让Socket推送给对应的Agent
 */
@Component
public class SocketClient {

    private final RestTemplate restTemplate;
    private final String socketBaseUrl;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(SocketClient.class);

    public SocketClient(RestTemplate restTemplate,
                        @Value("${socket.server.url:http://localhost:9201}") String socketBaseUrl,
                        ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.socketBaseUrl = socketBaseUrl;
        this.objectMapper = objectMapper;
        log.info("SocketClient initialized with server: {}", socketBaseUrl);
    }

    /**
     * 向指定的Agent转发命令
     * 通过调用Socket服务的命令转发API
     * @param agentId 代理ID（Agent标识）
     * @param commandId 命令ID（数据库主键）
     * @param commandType 命令类型
     * @param commandContent 命令内容
     * @return true表示转发成功，false表示失败
     */
    public boolean forwardCommandToAgent(String agentId, Long commandId, String commandType, String commandContent) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(socketBaseUrl)
                    .path("/api/socket/command/forward/{agentId}")
                    .buildAndExpand(agentId)
                    .toUriString();

            // 构建命令消息
            Map<String, Object> command = new HashMap<>();
            command.put("type", "command");
            command.put("commandId", commandId);
            command.put("commandType", commandType);
            command.put("commandContent", commandContent);
            command.put("timestamp", System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(command, headers);

            restTemplate.postForObject(url, entity, String.class);
            log.info("Command forwarded to agent via Socket: agentId={}, commandType={}", agentId, commandType);
            return true;
        } catch (Exception e) {
            log.error("Error forwarding command to agent: agentId={}, commandType={}", agentId, commandType, e);
            return false;
        }
    }

}
