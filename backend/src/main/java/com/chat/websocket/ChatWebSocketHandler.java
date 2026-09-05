package com.chat.websocket;

import com.chat.repository.MessageRepository;
import com.chat.security.TokenService;
import com.chat.service.ValidationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConnectionManager connectionManager;
    private final TokenService tokenService;
    private final ValidationService validationService;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatWebSocketHandler(ConnectionManager connectionManager,
                                TokenService tokenService,
                                ValidationService validationService,
                                MessageRepository messageRepository) {
        this.connectionManager = connectionManager;
        this.tokenService = tokenService;
        this.validationService = validationService;
        this.messageRepository = messageRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        boolean registered = connectionManager.register(username, session);
        if (!registered) {
            session.close(CloseStatus.SERVICE_OVERLOAD);
            return;
        }

        // Gửi thông báo chào mừng cho chính user
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("type", "system");
        welcome.put("message", "Chào mừng " + username + "! Đã vào phòng chung.");
        connectionManager.sendDirect(session, welcome);

        // Cập nhật danh sách online tới tất cả mọi người
        broadcastOnlineUsers();

        // Báo cho các client khác trong phòng chung
        Map<String, Object> joinMsg = new HashMap<>();
        joinMsg.put("type", "system");
        joinMsg.put("message", username + " đã tham gia phòng chung");
        connectionManager.broadcastToPublic(joinMsg, username);

        // Khôi phục các phiên chat riêng đang hoạt động (ví dụ khi F5 reload trang)
        Set<String> activePartners = connectionManager.getPrivatePartners(username);
        for (String partner : activePartners) {
            Map<String, Object> restoreMsg = new HashMap<>();
            restoreMsg.put("type", "private_session_started");
            restoreMsg.put("with", partner);
            restoreMsg.put("restored", true);
            connectionManager.sendDirect(session, restoreMsg);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String username = connectionManager.getUsernameBySessionId(session.getId());

        if (username == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        JsonNode json = objectMapper.readTree(payload);
        String type = json.path("type").asText("");

        switch (type.toLowerCase()) {
            case "public_message":
                handlePublicMessage(username, session, json.path("message").asText(""));
                break;
            case "start_private_chat":
                handleStartPrivateChat(username, session, json.path("target").asText(""));
                break;
            case "private_request":
                String reqMessage = json.has("message") ? json.path("message").asText("") : json.path("preview").asText("");
                if (reqMessage.isEmpty()) {
                    handleStartPrivateChat(username, session, json.path("target").asText(""));
                } else {
                    handlePrivateRequest(username, session, json.path("target").asText(""), reqMessage);
                }
                break;
            case "private_accept":
            case "accept_request":
                String requester = json.has("from") ? json.path("from").asText("") :
                                   (json.has("sender") ? json.path("sender").asText("") : json.path("target").asText(""));
                handlePrivateAccept(username, session, requester);
                break;
            case "private_decline":
            case "decline_request":
                String decRequester = json.has("from") ? json.path("from").asText("") :
                                      (json.has("sender") ? json.path("sender").asText("") : json.path("target").asText(""));
                handlePrivateDecline(username, session, decRequester);
                break;
            case "private_message":
                String pTarget = json.path("target").asText("");
                String pMsg = json.path("message").asText("");
                handlePrivateMessage(username, session, pTarget, pMsg);
                break;
            case "end_private_chat":
                String endTarget = json.has("target") ? json.path("target").asText("") : json.path("with").asText("");
                handleEndPrivateChat(username, session, endTarget);
                break;
            case "back_to_public":
                handleBackToPublic(username, session);
                break;
            default:
                logger.warn("[WS UNKNOWN TYPE] {} gửi type không xác định: {}", username, type);
                break;
        }
    }

    private void handlePublicMessage(String username, WebSocketSession session, String text) {
        ValidationService.ValidationResult val = validationService.validateMessage(text);
        if (!val.isValid()) {
            sendError(session, val.getMessage());
            return;
        }

        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        messageRepository.savePublicMessage(username, text, timestamp);

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "message");
        msg.put("room", "public");
        msg.put("sender", username);
        msg.put("message", text);
        msg.put("timestamp", timestamp);

        connectionManager.broadcastToPublic(msg, null);
        logger.info("[PUBLIC CHAT] {}: {}", username, text);
    }

    private void handlePrivateRequest(String sender, WebSocketSession session, String target, String previewText) {
        if (target == null || target.trim().isEmpty()) {
            sendError(session, "Tên người nhận không hợp lệ");
            return;
        }
        target = target.trim();

        if (target.equalsIgnoreCase(sender)) {
            sendError(session, "Không thể gửi yêu cầu chat riêng cho chính mình");
            return;
        }

        if (!connectionManager.isUserOnline(target)) {
            sendError(session, "Lỗi: " + target + " không online");
            return;
        }

        ValidationService.ValidationResult val = validationService.validateMessage(previewText);
        if (!val.isValid()) {
            sendError(session, val.getMessage());
            return;
        }

        connectionManager.addPendingRequest(sender, target, previewText);

        Map<String, Object> reqMsg = new HashMap<>();
        reqMsg.put("type", "private_request");
        reqMsg.put("from", sender);
        reqMsg.put("preview", previewText);
        reqMsg.put("timeout", ConnectionManager.REQUEST_TIMEOUT_SECONDS);
        connectionManager.sendMessage(target, reqMsg);

        sendSystemMessage(session, "Đã gửi yêu cầu tới " + target + " (hết hạn sau " + ConnectionManager.REQUEST_TIMEOUT_SECONDS + "s)");
        logger.info("[YÊU CẦU CHAT RIÊNG] {} -> {}", sender, target);
    }

    private void handleStartPrivateChat(String sender, WebSocketSession session, String target) {
        if (target == null || target.trim().isEmpty()) {
            sendError(session, "Tên người nhận không hợp lệ");
            return;
        }
        target = target.trim();

        if (target.equalsIgnoreCase(sender)) {
            sendError(session, "Không thể gửi yêu cầu chat riêng cho chính mình");
            return;
        }

        if (!connectionManager.isUserOnline(target)) {
            sendError(session, "Lỗi: " + target + " không online");
            return;
        }

        // Bắt đầu phiên chat riêng ngay lập tức (không cần chờ duyệt)
        connectionManager.startPrivateSession(sender, target);

        // Gửi event private_session_started cho người khởi tạo
        Map<String, Object> startForSender = new HashMap<>();
        startForSender.put("type", "private_session_started");
        startForSender.put("with", target);
        connectionManager.sendDirect(session, startForSender);

        // Gửi event private_session_started cho đối phương (để tab xuất hiện ở danh sách chat)
        UserSession targetSession = connectionManager.getSession(target);
        if (targetSession != null && targetSession.getSession().isOpen()) {
            Map<String, Object> startForTarget = new HashMap<>();
            startForTarget.put("type", "private_session_started");
            startForTarget.put("with", sender);
            startForTarget.put("restored", true);
            connectionManager.sendDirect(targetSession.getSession(), startForTarget);
        }

        // Gửi room_state để tương thích ngược cho cả hai bên
        Map<String, Object> stateForSender = new HashMap<>();
        stateForSender.put("type", "room_state");
        stateForSender.put("room", "private");
        stateForSender.put("target", target);
        connectionManager.sendDirect(session, stateForSender);

        if (targetSession != null && targetSession.getSession().isOpen()) {
            Map<String, Object> stateForTarget = new HashMap<>();
            stateForTarget.put("type", "room_state");
            stateForTarget.put("room", "private");
            stateForTarget.put("target", sender);
            connectionManager.sendDirect(targetSession.getSession(), stateForTarget);
        }

        sendSystemMessage(session, "Đã mở cuộc trò chuyện riêng với " + target + ".");
        logger.info("[TRỰC TIẾP BẮT ĐẦU CHAT RIÊNG] {} <-> {}", sender, target);
    }

    private void handlePrivateAccept(String accepter, WebSocketSession session, String requester) {
        if (requester == null || requester.trim().isEmpty()) {
            sendError(session, "Tên không hợp lệ");
            return;
        }
        requester = requester.trim();

        connectionManager.removePendingRequest(requester, accepter);

        if (!connectionManager.isUserOnline(requester)) {
            sendError(session, "Lỗi: " + requester + " đã offline");
            return;
        }

        handleStartPrivateChat(accepter, session, requester);
    }

    private void handlePrivateDecline(String decliner, WebSocketSession session, String requester) {
        if (requester == null || requester.trim().isEmpty()) {
            sendError(session, "Tên không hợp lệ");
            return;
        }
        requester = requester.trim();

        PrivateRequest req = connectionManager.removePendingRequest(requester, decliner);
        if (req == null) {
            sendError(session, "Không có yêu cầu từ " + requester + " (có thể đã hết hạn)");
            return;
        }

        sendSystemMessage(session, "Đã từ chối " + requester);

        Map<String, Object> notifyRequester = new HashMap<>();
        notifyRequester.put("type", "system");
        notifyRequester.put("message", decliner + " đã từ chối yêu cầu chat riêng");
        connectionManager.sendMessage(requester, notifyRequester);

        logger.info("[TỪ CHỐI CHAT RIÊNG] {} từ chối {}", decliner, requester);
    }

    private void handlePrivateMessage(String sender, WebSocketSession session, String target, String text) {
        if (target == null || target.trim().isEmpty()) {
            UserSession us = connectionManager.getSession(sender);
            if (us != null) {
                target = us.getActivePrivateTarget();
            }
        }

        if (target == null || target.trim().isEmpty()) {
            sendError(session, "Chưa xác định người nhận tin nhắn riêng");
            return;
        }
        target = target.trim();

        if (!connectionManager.hasPrivateSession(sender, target)) {
            connectionManager.startPrivateSession(sender, target);
            UserSession targetSession = connectionManager.getSession(target);
            if (targetSession != null && targetSession.getSession().isOpen()) {
                Map<String, Object> startForTarget = new HashMap<>();
                startForTarget.put("type", "private_session_started");
                startForTarget.put("with", sender);
                startForTarget.put("restored", true);
                connectionManager.sendDirect(targetSession.getSession(), startForTarget);
            }
        }

        if (!connectionManager.isUserOnline(target)) {
            sendError(session, "Đối phương không còn online");
            return;
        }

        ValidationService.ValidationResult val = validationService.validateMessage(text);
        if (!val.isValid()) {
            sendError(session, val.getMessage());
            return;
        }

        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        messageRepository.savePrivateMessage(sender, target, text, timestamp);

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "message");
        msg.put("room", "private");
        msg.put("sender", sender);
        msg.put("receiver", target);
        msg.put("message", text);
        msg.put("timestamp", timestamp);

        // Gửi tới đúng 2 client
        connectionManager.sendDirect(session, msg);
        connectionManager.sendMessage(target, msg);

        logger.info("[PRIVATE CHAT] {} -> {}: {}", sender, target, text);
    }

    private void handleEndPrivateChat(String sender, WebSocketSession session, String target) {
        if (target == null || target.trim().isEmpty()) {
            UserSession us = connectionManager.getSession(sender);
            if (us != null) {
                target = us.getActivePrivateTarget();
            }
        }

        if (target == null || target.trim().isEmpty()) {
            sendError(session, "Không tìm thấy phiên chat riêng để kết thúc");
            return;
        }
        target = target.trim();

        connectionManager.endPrivateSession(sender, target);

        // Gửi event private_session_ended cho sender
        Map<String, Object> endMsgForSender = new HashMap<>();
        endMsgForSender.put("type", "private_session_ended");
        endMsgForSender.put("with", target);
        endMsgForSender.put("endedBy", sender);
        connectionManager.sendDirect(session, endMsgForSender);

        // Gửi event private_session_ended cho partner
        Map<String, Object> endMsgForPartner = new HashMap<>();
        endMsgForPartner.put("type", "private_session_ended");
        endMsgForPartner.put("with", sender);
        endMsgForPartner.put("endedBy", sender);
        connectionManager.sendMessage(target, endMsgForPartner);

        // Gửi room_state public để tương thích ngược
        Map<String, Object> statePublic = new HashMap<>();
        statePublic.put("type", "room_state");
        statePublic.put("room", "public");
        statePublic.put("target", null);
        connectionManager.sendDirect(session, statePublic);
        connectionManager.sendMessage(target, statePublic);

        // Gửi system message
        sendSystemMessage(session, "Đã kết thúc phiên chat riêng với " + target);
        UserSession partnerSession = connectionManager.getSession(target);
        if (partnerSession != null && partnerSession.getSession().isOpen()) {
            sendSystemMessage(partnerSession.getSession(), sender + " đã kết thúc phiên chat riêng");
        }

        broadcastOnlineUsers();
        logger.info("[KẾT THÚC CHAT RIÊNG] {} kết thúc chat riêng với {}", sender, target);
    }

    private void handleBackToPublic(String username, WebSocketSession session) {
        UserSession us = connectionManager.getSession(username);
        String partner = (us != null) ? us.getActivePrivateTarget() : null;
        if (partner != null && connectionManager.hasPrivateSession(username, partner)) {
            handleEndPrivateChat(username, session, partner);
        } else {
            Map<String, Object> state = new HashMap<>();
            state.put("type", "room_state");
            state.put("room", "public");
            state.put("target", null);
            connectionManager.sendDirect(session, state);
            sendSystemMessage(session, "Đã quay lại phòng chung.");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        UserSession removed = connectionManager.removeBySessionId(session.getId());
        if (removed != null) {
            String username = removed.getUsername();

            broadcastOnlineUsers();

            Map<String, Object> leaveMsg = new HashMap<>();
            leaveMsg.put("type", "system");
            leaveMsg.put("message", username + " đã rời phòng chung");
            connectionManager.broadcastToPublic(leaveMsg, null);
        }
    }

    private void sendError(WebSocketSession session, String message) {
        Map<String, Object> err = new HashMap<>();
        err.put("type", "error");
        err.put("message", message);
        connectionManager.sendDirect(session, err);
    }

    private void sendSystemMessage(WebSocketSession session, String message) {
        Map<String, Object> sys = new HashMap<>();
        sys.put("type", "system");
        sys.put("message", message);
        connectionManager.sendDirect(session, sys);
    }

    private void broadcastOnlineUsers() {
        Map<String, Object> onlineMsg = new HashMap<>();
        onlineMsg.put("type", "online_users");
        onlineMsg.put("users", connectionManager.getOnlineUsersDetails());
        onlineMsg.put("usernames", connectionManager.getOnlineUsernames());
        connectionManager.broadcastAll(onlineMsg);
    }
}
