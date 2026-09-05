package com.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    public static final int MAX_CLIENTS = 5;

    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUsername = new ConcurrentHashMap<>();
    private final Map<String, PrivateRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Set<String> activePrivateSessions = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> userPrivatePartners = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final long REQUEST_TIMEOUT_SECONDS = 60;

    private String getRequestKey(String sender, String receiver) {
        return sender.toLowerCase() + "->" + receiver.toLowerCase();
    }

    public void addPendingRequest(String sender, String receiver, String preview) {
        pendingRequests.put(getRequestKey(sender, receiver), new PrivateRequest(sender, receiver, preview));
    }

    public PrivateRequest getPendingRequest(String sender, String receiver) {
        return pendingRequests.get(getRequestKey(sender, receiver));
    }

    public PrivateRequest removePendingRequest(String sender, String receiver) {
        return pendingRequests.remove(getRequestKey(sender, receiver));
    }

    public boolean cancelPendingRequest(String sender, String receiver) {
        PrivateRequest req = removePendingRequest(sender, receiver);
        if (req != null) {
            Map<String, Object> cancelMsg = new HashMap<>();
            cancelMsg.put("type", "error");
            cancelMsg.put("message", "Yêu cầu chat riêng giữa " + sender + " và " + receiver + " đã bị hủy bởi quản trị viên");
            sendMessage(sender, cancelMsg);
            sendMessage(receiver, cancelMsg);
            return true;
        }
        return false;
    }

    public List<PrivateRequest> cleanupExpiredRequests() {
        List<PrivateRequest> expired = new ArrayList<>();
        for (Map.Entry<String, PrivateRequest> entry : pendingRequests.entrySet()) {
            if (entry.getValue().isExpired(REQUEST_TIMEOUT_SECONDS)) {
                expired.add(entry.getValue());
                pendingRequests.remove(entry.getKey());
            }
        }
        return expired;
    }

    public List<Map<String, Object>> getAllPendingRequests() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PrivateRequest req : pendingRequests.values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("sender", req.getSender());
            map.put("receiver", req.getReceiver());
            map.put("preview", req.getPreview());
            map.put("createdAt", req.getCreatedAt());
            map.put("remainingSeconds", req.getRemainingSeconds(REQUEST_TIMEOUT_SECONDS));
            list.add(map);
        }
        return list;
    }

    public int getPendingRequestsCount() {
        return pendingRequests.size();
    }

    public void setRoomState(String username, String roomType, String roomTarget) {
        UserSession us = activeSessions.get(username);
        if (us != null) {
            us.setRoomType(roomType);
            us.setRoomTarget(roomTarget);
            logger.info("[TRẠNG THÁI PHÒNG] {} chuyển sang: {} (với {})", username, roomType, roomTarget);
        }
    }

    public boolean isUserOnline(String username) {
        return activeSessions.containsKey(username);
    }

    public synchronized boolean canAcceptNewConnection(String username) {
        // Nếu user này đã có kết nối cũ thì cho phép thay thế (không tính thêm client mới)
        if (activeSessions.containsKey(username)) {
            return true;
        }
        return activeSessions.size() < MAX_CLIENTS;
    }

    public synchronized boolean register(String username, WebSocketSession session) {
        if (!canAcceptNewConnection(username)) {
            return false;
        }

        // Đóng session cũ nếu user này đăng nhập lại trên tab khác
        UserSession oldSession = activeSessions.get(username);
        if (oldSession != null && oldSession.getSession().isOpen()) {
            try {
                oldSession.getSession().close();
            } catch (IOException ignored) {}
            sessionToUsername.remove(oldSession.getSession().getId());
        }

        UserSession userSession = new UserSession(username, session);
        // Khôi phục target nếu user này đang có partner
        Set<String> partners = userPrivatePartners.get(username.toLowerCase());
        if (partners != null && !partners.isEmpty()) {
            userSession.setActivePrivateTarget(partners.iterator().next());
        }

        activeSessions.put(username, userSession);
        sessionToUsername.put(session.getId(), username);
        logger.info("[WS CONNECT] User '{}' kết nối thành công. Tổng online: {}/{}", username, activeSessions.size(), MAX_CLIENTS);
        return true;
    }

    public synchronized UserSession removeBySessionId(String sessionId) {
        String username = sessionToUsername.remove(sessionId);
        if (username != null) {
            UserSession current = activeSessions.get(username);
            if (current != null && current.getSession().getId().equals(sessionId)) {
                UserSession userSession = activeSessions.remove(username);
                logger.info("[WS DISCONNECT] User '{}' ngắt kết nối thực sự. Tổng online: {}/{}", username, activeSessions.size(), MAX_CLIENTS);
                return userSession;
            } else {
                logger.info("[WS DISCONNECT] Session cũ của user '{}' đã đóng (user đã có kết nối mới)", username);
                return null;
            }
        }
        return null;
    }

    public boolean disconnectUser(String username, String reason) {
        UserSession us = activeSessions.get(username);
        if (us != null && us.getSession().isOpen()) {
            try {
                Map<String, Object> kickedMsg = new HashMap<>();
                kickedMsg.put("type", "kicked");
                kickedMsg.put("reason", (reason != null && !reason.trim().isEmpty()) ? reason : "Bạn đã bị quản trị viên ngắt kết nối");
                sendDirect(us.getSession(), kickedMsg);

                // Đóng session với POLICY_VIOLATION
                us.getSession().close(CloseStatus.POLICY_VIOLATION);
                logger.info("[ADMIN KICK] Đã cưỡng chế ngắt kết nối user '{}', lý do: {}", username, reason);
                return true;
            } catch (Exception e) {
                logger.error("[ADMIN KICK LỖI] {}", e.getMessage());
            }
        }
        return false;
    }

    public UserSession getSession(String username) {
        return activeSessions.get(username);
    }

    public String getUsernameBySessionId(String sessionId) {
        return sessionToUsername.get(sessionId);
    }

    public int getClientCount() {
        return activeSessions.size();
    }

    public int getUsersInPublicCount() {
        // Tất cả user đang online đều là thành viên của phòng chung
        return activeSessions.size();
    }

    public String getPairKey(String u1, String u2) {
        String a = u1.toLowerCase();
        String b = u2.toLowerCase();
        return a.compareTo(b) < 0 ? a + "<->" + b : b + "<->" + a;
    }

    public synchronized boolean startPrivateSession(String u1, String u2) {
        String key = getPairKey(u1, u2);
        activePrivateSessions.add(key);
        userPrivatePartners.computeIfAbsent(u1.toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(u2);
        userPrivatePartners.computeIfAbsent(u2.toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(u1);

        UserSession us1 = activeSessions.get(u1);
        if (us1 != null) {
            us1.setActivePrivateTarget(u2);
        }
        UserSession us2 = activeSessions.get(u2);
        if (us2 != null) {
            us2.setActivePrivateTarget(u1);
        }
        logger.info("[PRIVATE SESSION BẮT ĐẦU] {} <-> {}", u1, u2);
        return true;
    }

    public synchronized boolean endPrivateSession(String u1, String u2) {
        String key = getPairKey(u1, u2);
        boolean removed = activePrivateSessions.remove(key);
        Set<String> p1 = userPrivatePartners.get(u1.toLowerCase());
        if (p1 != null) {
            p1.remove(u2);
            if (p1.isEmpty()) userPrivatePartners.remove(u1.toLowerCase());
        }
        Set<String> p2 = userPrivatePartners.get(u2.toLowerCase());
        if (p2 != null) {
            p2.remove(u1);
            if (p2.isEmpty()) userPrivatePartners.remove(u2.toLowerCase());
        }

        UserSession us1 = activeSessions.get(u1);
        if (us1 != null && u2.equalsIgnoreCase(us1.getActivePrivateTarget())) {
            us1.setActivePrivateTarget(null);
        }
        UserSession us2 = activeSessions.get(u2);
        if (us2 != null && u1.equalsIgnoreCase(us2.getActivePrivateTarget())) {
            us2.setActivePrivateTarget(null);
        }
        logger.info("[PRIVATE SESSION KẾT THÚC] {} <-> {}", u1, u2);
        return removed;
    }

    public boolean hasPrivateSession(String u1, String u2) {
        return activePrivateSessions.contains(getPairKey(u1, u2));
    }

    public Set<String> getPrivatePartners(String username) {
        Set<String> set = userPrivatePartners.get(username.toLowerCase());
        return set != null ? new HashSet<>(set) : Collections.emptySet();
    }

    public int getPrivatePairsCount() {
        return activePrivateSessions.size();
    }

    public List<List<String>> getActivePrivatePairsList() {
        List<List<String>> list = new ArrayList<>();
        for (String pair : activePrivateSessions) {
            String[] parts = pair.split("<->");
            if (parts.length == 2) {
                list.add(Arrays.asList(parts[0], parts[1]));
            }
        }
        return list;
    }

    public List<String> getOnlineUsernames() {
        return new ArrayList<>(activeSessions.keySet());
    }

    public List<Map<String, Object>> getOnlineUsersDetails() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (UserSession us : activeSessions.values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("username", us.getUsername());
            map.put("roomType", us.getRoomType());
            map.put("roomTarget", us.getRoomTarget());
            map.put("connectedAt", us.getConnectedAt());
            map.put("remoteAddress", us.getRemoteAddress());
            list.add(map);
        }
        return list;
    }

    public Map<String, Object> getRoomsOverview() {
        List<String> publicUsers = new ArrayList<>(activeSessions.keySet());
        List<List<String>> privatePairs = getActivePrivatePairsList();

        Map<String, Object> result = new HashMap<>();
        result.put("public", publicUsers);
        result.put("private_pairs", privatePairs);
        return result;
    }

    public void sendMessage(String username, Object payload) {
        UserSession us = activeSessions.get(username);
        if (us != null && us.getSession().isOpen()) {
            sendDirect(us.getSession(), payload);
        }
    }

    public void sendDirect(WebSocketSession session, Object payload) {
        if (session != null && session.isOpen()) {
            synchronized (session) {
                try {
                    if (session.isOpen()) {
                        String json = objectMapper.writeValueAsString(payload);
                        session.sendMessage(new TextMessage(json));
                    }
                } catch (Exception e) {
                    logger.debug("[WS SEND] Không thể gửi tin (session có thể đã đóng): {}", e.getMessage());
                }
            }
        }
    }

    public void broadcastToPublic(Object payload, String excludeUsername) {
        // TẤT CẢ user đang online đều thuộc phòng chung và nhận được public message
        for (UserSession us : activeSessions.values()) {
            if (excludeUsername == null || !us.getUsername().equalsIgnoreCase(excludeUsername)) {
                sendDirect(us.getSession(), payload);
            }
        }
    }

    public void broadcastAll(Object payload) {
        for (UserSession us : activeSessions.values()) {
            sendDirect(us.getSession(), payload);
        }
    }

    public synchronized void resetForTesting() {
        for (UserSession us : activeSessions.values()) {
            try {
                if (us.getSession().isOpen()) {
                    us.getSession().close();
                }
            } catch (Exception ignored) {}
        }
        activeSessions.clear();
        sessionToUsername.clear();
        pendingRequests.clear();
        activePrivateSessions.clear();
        userPrivatePartners.clear();
    }
}
