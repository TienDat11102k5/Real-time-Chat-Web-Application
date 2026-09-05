package com.chat.websocket;

import com.chat.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthHandshakeInterceptor.class);

    private final TokenService tokenService;
    private final ConnectionManager connectionManager;

    public AuthHandshakeInterceptor(TokenService tokenService, ConnectionManager connectionManager) {
        this.tokenService = tokenService;
        this.connectionManager = connectionManager;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        URI uri = request.getURI();
        String query = uri.getQuery();
        String token = null;

        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "token".equalsIgnoreCase(pair[0])) {
                    token = pair[1];
                    break;
                }
            }
        }

        if (token == null || !tokenService.validateToken(token)) {
            logger.warn("[HANDSHAKE REJECT] Token không hợp lệ từ {}", request.getRemoteAddress());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String username = tokenService.getUsernameFromToken(token);
        if (username == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        if (!connectionManager.canAcceptNewConnection(username)) {
            logger.warn("[HANDSHAKE REJECT] Server đã đầy giới hạn {} clients", ConnectionManager.MAX_CLIENTS);
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            return false;
        }

        // Lưu username đã xác thực vào session attributes
        attributes.put("username", username);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
