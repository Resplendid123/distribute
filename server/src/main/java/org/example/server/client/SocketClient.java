package org.example.server.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
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
    private static final Logger log = LoggerFactory.getLogger(SocketClient.class);

    public SocketClient(RestTemplate restTemplate,
                        @Value("${socket.server.url:http://localhost:9201}") String socketBaseUrl) {
        this.restTemplate = restTemplate;
        this.socketBaseUrl = socketBaseUrl;
        log.info("SocketClient initialized with server: {}", socketBaseUrl);
    }

    /**
     * 向指定的Agent转发命令
     * 通过调用Socket服务的命令转发API
     * @param deviceId 设备ID（作为Agent标识）
     * @param commandId 命令ID（数据库主键）
     * @param commandType 命令类型
     * @param commandContent 命令内容
     * @return true表示转发成功，false表示失败或Agent不在线
     */
    public boolean forwardCommandToAgent(Long deviceId, Long commandId, String commandType, String commandContent) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(socketBaseUrl)
                    .path("/api/socket/command/forward/{deviceId}")
                    .build(deviceId);

            Map<String, Object> command = new HashMap<>();
            command.put("type", "command");
            command.put("commandId", commandId);
            command.put("commandType", commandType);
            command.put("commandContent", commandContent);
            command.put("timestamp", System.currentTimeMillis());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(command, headers);

            restTemplate.postForObject(uri, entity, String.class);
            log.info("Command forwarded to agent via Socket: deviceId={}, commandType={}", deviceId, commandType);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Agent not found or offline (expected when agent is disconnected): deviceId={}, commandType={}", deviceId, commandType);
            return false;
        } catch (HttpClientErrorException e) {
            log.warn("HTTP error forwarding command to agent: deviceId={}, status={}, error={}", 
                deviceId, e.getStatusCode(), e.getStatusText());
            return false;
        } catch (ResourceAccessException e) {
            log.warn("Socket service unreachable: {}", socketBaseUrl);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error forwarding command to agent: deviceId={}, commandType={}", deviceId, commandType, e);
            return false;
        }
    }

}
