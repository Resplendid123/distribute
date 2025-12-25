package org.example.socket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手拦截器
 * 在WebSocket握手阶段获取客户端真实IP和端口
 */
public class AgentWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            // 获取客户端真实 IP:PORT
            String clientIpPort = extractClientIpPort(request);
            
            // 存储到attributes中，方便后续在WebSocket Session中获取
            attributes.put("clientIpPort", clientIpPort);
            log.info("Extracted client IP:PORT during WebSocket handshake: {}", clientIpPort);
            
            // 从路径中提取deviceId
            String path = request.getURI().getPath();
            if (path.contains("/ws/agent/")) {
                String deviceId = path.substring(path.lastIndexOf("/") + 1);
                attributes.put("deviceId", deviceId);
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error extracting client IP:PORT from handshake request", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("Error after WebSocket handshake", exception);
        }
    }

    /**
     * 从握手请求中提取客户端真实IP和端口
     * 支持代理环境（X-Forwarded-For, X-Real-IP等）
     */
    private String extractClientIpPort(ServerHttpRequest request) throws RuntimeException {
        try {
            // 方式1: 尝试从HTTP头获取代理后的真实IP
            String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                String ip = xForwardedFor.split(",")[0].trim();
                log.debug("Found X-Forwarded-For: {}", ip);
                return ip + ":0";
            }
            
            // 方式2: 尝试从X-Real-IP头获取
            String xRealIp = request.getHeaders().getFirst("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                log.debug("Found X-Real-IP: {}", xRealIp);
                return xRealIp + ":0";
            }
            
            // 方式3: 从远程地址获取
            java.net.InetSocketAddress remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null) {
                String ip = remoteAddress.getAddress().getHostAddress();
                int port = remoteAddress.getPort();
                String ipPort = ip + ":" + port;
                log.debug("Found remote address: {}", ipPort);
                return ipPort;
            }
            
            throw new RuntimeException("Cannot extract client IP:PORT from WebSocket handshake request");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error extracting IP:PORT: " + e.getMessage(), e);
        }
    }
}
