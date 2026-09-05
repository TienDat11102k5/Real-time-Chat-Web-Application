package com.chat.controller;

import com.chat.dto.ApiResponse;
import com.chat.dto.AuthResponse;
import com.chat.model.AuditLog;
import com.chat.model.ChatMessage;
import com.chat.model.User;
import com.chat.repository.AuditLogRepository;
import com.chat.repository.MessageRepository;
import com.chat.repository.UserRepository;
import com.chat.security.TokenService;
import com.chat.service.AuthService;
import com.chat.service.ValidationService;
import com.chat.websocket.ConnectionManager;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AuthService authService;
    private final TokenService tokenService;
    private final ValidationService validationService;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final AuditLogRepository auditLogRepository;
    private final ConnectionManager connectionManager;

    private static final long SERVER_START_TIME = System.currentTimeMillis();

    public AdminController(AuthService authService,
                           TokenService tokenService,
                           ValidationService validationService,
                           UserRepository userRepository,
                           MessageRepository messageRepository,
                           AuditLogRepository auditLogRepository,
                           ConnectionManager connectionManager) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.validationService = validationService;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.auditLogRepository = auditLogRepository;
        this.connectionManager = connectionManager;
    }

    private String getCallerAdmin(HttpServletRequest request) {
        Object attr = request.getAttribute("adminUsername");
        return (attr != null) ? attr.toString() : "admin";
    }

    private String getCallerRole(HttpServletRequest request) {
        Object attr = request.getAttribute("adminRole");
        return (attr != null) ? attr.toString() : "admin";
    }

    // ==========================================
    // B1. ĐĂNG NHẬP ADMIN
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        AuthResponse resp = authService.adminLogin(username, password);
        if (resp.isOk()) {
            auditLogRepository.record(resp.getUsername(), "ADMIN_LOGIN", "system", "SUCCESS", "Đăng nhập thành công");
            return ResponseEntity.ok(resp);
        } else {
            auditLogRepository.record(username != null ? username : "unknown", "ADMIN_LOGIN", "system", "FAIL", resp.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }
    }

    // ==========================================
    // B3. ADMIN DASHBOARD STATS
    // ==========================================
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        int onlineCount = connectionManager.getClientCount();
        int maxClients = ConnectionManager.MAX_CLIENTS;
        int publicUsers = connectionManager.getUsersInPublicCount();
        int privatePairs = connectionManager.getPrivatePairsCount();
        int pendingReqs = connectionManager.getPendingRequestsCount();

        long uptimeSeconds = (System.currentTimeMillis() - SERVER_START_TIME) / 1000;

        stats.put("online_users", onlineCount);
        stats.put("max_clients", maxClients);
        stats.put("public_room_users", publicUsers);
        stats.put("private_pairs", privatePairs);
        stats.put("pending_requests", pendingReqs);
        stats.put("database", "ok");
        stats.put("uptime_seconds", uptimeSeconds);
        stats.put("total_accounts", userRepository.count());
        stats.put("public_messages_count", messageRepository.countPublicMessages());
        stats.put("private_messages_count", messageRepository.countPrivateMessages());

        return ResponseEntity.ok(stats);
    }

    // ==========================================
    // B4. USERS ONLINE & KICK
    // ==========================================
    @GetMapping("/users")
    public ResponseEntity<?> getOnlineUsers() {
        return ResponseEntity.ok(connectionManager.getOnlineUsersDetails());
    }

    @PostMapping("/users/{username}/disconnect")
    public ResponseEntity<?> disconnectUser(@PathVariable String username,
                                           @RequestBody(required = false) Map<String, String> body,
                                           HttpServletRequest request) {
        String admin = getCallerAdmin(request);
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Bị quản trị viên ngắt kết nối";

        boolean disconnected = connectionManager.disconnectUser(username, reason);
        if (disconnected) {
            auditLogRepository.record(admin, "DISCONNECT_USER", username, "SUCCESS", "Lý do: " + reason);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã ngắt kết nối user '" + username + "'"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", "User '" + username + "' hiện không online"));
        }
    }

    // ==========================================
    // B5. USER ACCOUNTS MANAGEMENT
    // ==========================================
    @GetMapping("/accounts")
    public ResponseEntity<?> getAccounts(@RequestParam(required = false) String query,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int limit) {
        List<User> users = userRepository.searchAccounts(query, page, limit);
        int total = userRepository.countAccounts(query);
        int totalPages = (int) Math.ceil((double) total / limit);

        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("createdAt", u.getCreatedAt());
            map.put("role", u.getRole());
            map.put("status", u.getStatus());
            map.put("lockedAt", u.getLockedAt());
            map.put("deletedAt", u.getDeletedAt());
            map.put("isOnline", connectionManager.isUserOnline(u.getUsername()));
            list.add(map);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("accounts", list);
        result.put("total", total);
        result.put("page", page);
        result.put("limit", limit);
        result.put("totalPages", totalPages);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/accounts/{username}")
    public ResponseEntity<?> getAccountDetail(@PathVariable String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "error", "message", "Tài khoản không tồn tại"));
        }

        User u = userOpt.get();
        Map<String, Object> map = new HashMap<>();
        map.put("id", u.getId());
        map.put("username", u.getUsername());
        map.put("createdAt", u.getCreatedAt());
        map.put("role", u.getRole());
        map.put("status", u.getStatus());
        map.put("lockedAt", u.getLockedAt());
        map.put("deletedAt", u.getDeletedAt());
        map.put("isOnline", connectionManager.isUserOnline(u.getUsername()));
        map.put("publicMessagesCount", messageRepository.countPublicMessagesByUser(username));
        map.put("privateMessagesCount", messageRepository.countPrivateMessagesByUser(username));

        return ResponseEntity.ok(map);
    }

    @PostMapping("/accounts/{username}/kick")
    public ResponseEntity<?> kickAccount(@PathVariable String username,
                                         @RequestBody(required = false) Map<String, String> body,
                                         HttpServletRequest request) {
        return disconnectUser(username, body, request);
    }

    @PostMapping("/accounts/{username}/lock")
    public ResponseEntity<?> lockAccount(@PathVariable String username, HttpServletRequest request) {
        String admin = getCallerAdmin(request);
        if (admin.equalsIgnoreCase(username)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", "Không thể tự khóa tài khoản của chính mình"));
        }

        boolean locked = userRepository.lockUser(username);
        if (locked) {
            // Ngắt kết nối nếu user đang online
            connectionManager.disconnectUser(username, "Tài khoản của bạn đã bị khóa bởi quản trị viên");
            auditLogRepository.record(admin, "LOCK_USER", username, "SUCCESS", "Đã khóa tài khoản");
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã khóa tài khoản '" + username + "'"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", "Không thể khóa tài khoản"));
        }
    }

    @PostMapping("/accounts/{username}/unlock")
    public ResponseEntity<?> unlockAccount(@PathVariable String username, HttpServletRequest request) {
        String admin = getCallerAdmin(request);
        boolean unlocked = userRepository.unlockUser(username);
        if (unlocked) {
            auditLogRepository.record(admin, "UNLOCK_USER", username, "SUCCESS", "Đã mở khóa tài khoản");
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã mở khóa tài khoản '" + username + "'"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", "Không thể mở khóa tài khoản"));
        }
    }

    @PostMapping("/accounts/{username}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable String username,
                                           @RequestBody Map<String, String> body,
                                           HttpServletRequest request) {
        String admin = getCallerAdmin(request);
        String newPassword = body.get("new_password");

        ValidationService.ValidationResult passVal = validationService.validatePassword(newPassword);
        if (!passVal.isValid()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", passVal.getMessage()));
        }

        String hash = AuthService.hashPassword(newPassword.trim());
        boolean updated = userRepository.updatePassword(username, hash);
        if (updated) {
            auditLogRepository.record(admin, "RESET_PASSWORD", username, "SUCCESS", "Đã đặt lại mật khẩu mới");
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã đặt lại mật khẩu cho '" + username + "'"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", "Không thể đặt lại mật khẩu"));
        }
    }

    @PatchMapping("/accounts/{username}/role")
    public ResponseEntity<?> updateRole(@PathVariable String username,
                                        @RequestBody Map<String, String> body,
                                        HttpServletRequest request) {
        String admin = getCallerAdmin(request);
        String callerRole = getCallerRole(request);

        // Chỉ có admin mới được đổi role (moderator không được phép)
        if (!"admin".equalsIgnoreCase(callerRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "error", "message", "Chỉ Admin mới có quyền thay đổi role"));
        }

        String newRole = body.get("role");
        if (newRole == null || (!newRole.equalsIgnoreCase("user") && !newRole.equalsIgnoreCase("moderator") && !newRole.equalsIgnoreCase("admin"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", "Role phải là 'user', 'moderator' hoặc 'admin'"));
        }

        boolean updated = userRepository.updateRole(username, newRole.toLowerCase());
        if (updated) {
            auditLogRepository.record(admin, "CHANGE_ROLE", username, "SUCCESS", "Đổi sang role: " + newRole);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã cập nhật quyền của '" + username + "' thành " + newRole));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", "Không thể cập nhật role"));
        }
    }

    @DeleteMapping("/accounts/{username}")
    public ResponseEntity<?> deleteAccount(@PathVariable String username, HttpServletRequest request) {
        String admin = getCallerAdmin(request);
        String callerRole = getCallerRole(request);

        if (!"admin".equalsIgnoreCase(callerRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "error", "message", "Chỉ Admin mới có quyền xóa tài khoản"));
        }

        if (admin.equalsIgnoreCase(username)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", "Không thể tự xóa tài khoản admin đang đăng nhập"));
        }

        // Cưỡng chế ngắt kết nối nếu đang online
        connectionManager.disconnectUser(username, "Tài khoản của bạn đã bị xóa bởi quản trị viên");

        boolean deleted = userRepository.softDeleteUser(username);
        if (deleted) {
            auditLogRepository.record(admin, "DELETE_USER", username, "SUCCESS", "Đã soft-delete tài khoản");
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã xóa tài khoản '" + username + "' (Soft delete)"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", "Không thể xóa tài khoản"));
        }
    }

    @GetMapping("/accounts/{username}/messages")
    public ResponseEntity<?> getUserMessages(@PathVariable String username,
                                             @RequestParam(defaultValue = "50") int limit) {
        List<ChatMessage> messages = messageRepository.getUserRecentMessages(username, limit);
        return ResponseEntity.ok(messages);
    }

    // ==========================================
    // B9. ROOMS MANAGEMENT
    // ==========================================
    @GetMapping("/rooms")
    public ResponseEntity<?> getRooms() {
        return ResponseEntity.ok(connectionManager.getRoomsOverview());
    }

    // ==========================================
    // B10. PRIVATE REQUESTS MANAGEMENT
    // ==========================================
    @GetMapping("/private-requests")
    public ResponseEntity<?> getPrivateRequests() {
        return ResponseEntity.ok(connectionManager.getAllPendingRequests());
    }

    @DeleteMapping("/private-requests/{sender}/{receiver}")
    public ResponseEntity<?> cancelPrivateRequest(@PathVariable String sender,
                                                 @PathVariable String receiver,
                                                 HttpServletRequest request) {
        String admin = getCallerAdmin(request);
        boolean cancelled = connectionManager.cancelPendingRequest(sender, receiver);
        if (cancelled) {
            auditLogRepository.record(admin, "CANCEL_REQUEST", sender + "->" + receiver, "SUCCESS", "Hủy yêu cầu chat riêng");
            return ResponseEntity.ok(Map.of("status", "success", "message", "Đã hủy yêu cầu chat riêng giữa " + sender + " và " + receiver));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "Không tìm thấy yêu cầu giữa " + sender + " và " + receiver));
        }
    }

    // ==========================================
    // B11. SERVER LIMITS (READ-ONLY)
    // ==========================================
    @GetMapping("/limits")
    public ResponseEntity<?> getLimits() {
        Map<String, Object> limits = new HashMap<>();
        limits.put("MAX_CLIENTS", ConnectionManager.MAX_CLIENTS);
        limits.put("MAX_MESSAGE_LENGTH", ValidationService.MAX_MESSAGE_LENGTH);
        limits.put("MIN_USERNAME_LENGTH", ValidationService.MIN_USERNAME_LENGTH);
        limits.put("MAX_USERNAME_LENGTH", ValidationService.MAX_USERNAME_LENGTH);
        limits.put("MIN_PASSWORD_LENGTH", ValidationService.MIN_PASSWORD_LENGTH);
        limits.put("MAX_PASSWORD_LENGTH", ValidationService.MAX_PASSWORD_LENGTH);
        limits.put("REQUEST_TIMEOUT", ConnectionManager.REQUEST_TIMEOUT_SECONDS);
        limits.put("AUTH_TIMEOUT", 60);
        limits.put("CHAT_TIMEOUT", 1800);
        limits.put("CURRENT_CLIENTS", connectionManager.getClientCount());
        return ResponseEntity.ok(limits);
    }

    // ==========================================
    // B12 & B13. SERVER LOGS & AUDIT LOG
    // ==========================================
    @GetMapping("/logs")
    public ResponseEntity<?> getLogs(@RequestParam(defaultValue = "100") int limit,
                                     @RequestParam(required = false) String type) {
        List<AuditLog> logs = auditLogRepository.findRecent(limit, type);
        return ResponseEntity.ok(logs);
    }
}
