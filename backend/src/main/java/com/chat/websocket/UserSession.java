package com.chat.websocket;

import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserSession {
    private final String username;
    private final WebSocketSession session;
    private volatile String roomType; // "public" or "private"
    private volatile String roomTarget; // target username when private
    private final String connectedAt;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserSession(String username, WebSocketSession session) {
        this.username = username;
        this.session = session;
        this.roomType = "public";
        this.roomTarget = null;
        this.connectedAt = LocalDateTime.now().format(FORMATTER);
    }

    public String getUsername() {
        return username;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getRoomTarget() {
        return roomTarget;
    }

    public void setRoomTarget(String roomTarget) {
        this.roomTarget = roomTarget;
    }

    public String getActivePrivateTarget() {
        return roomTarget;
    }

    public void setActivePrivateTarget(String target) {
        this.roomTarget = target;
        this.roomType = (target != null && !target.isEmpty()) ? "private" : "public";
    }

    public String getConnectedAt() {
        return connectedAt;
    }

    public String getRemoteAddress() {
        try {
            if (session != null && session.getRemoteAddress() != null) {
                return session.getRemoteAddress().toString();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}
