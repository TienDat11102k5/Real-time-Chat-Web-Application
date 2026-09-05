package com.chat.service;

import com.chat.dto.ApiResponse;
import com.chat.dto.AuthResponse;
import com.chat.model.User;
import com.chat.repository.UserRepository;
import com.chat.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserRepository userRepository;
    private final ValidationService validationService;
    private final TokenService tokenService;

    @Value("${app.admin.username:admin}")
    private String envAdminUsername;

    @Value("${app.admin.password:admin123456}")
    private String envAdminPassword;

    public AuthService(UserRepository userRepository,
                       ValidationService validationService,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.validationService = validationService;
        this.tokenService = tokenService;
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public ApiResponse register(String username, String password) {
        ValidationService.ValidationResult userVal = validationService.validateUsername(username);
        if (!userVal.isValid()) {
            return ApiResponse.error(userVal.getMessage());
        }

        ValidationService.ValidationResult passVal = validationService.validatePassword(password);
        if (!passVal.isValid()) {
            return ApiResponse.error(passVal.getMessage());
        }

        String cleanUsername = username.trim();
        if (userRepository.existsByUsername(cleanUsername)) {
            return ApiResponse.error("Tên tài khoản đã tồn tại");
        }

        String hash = hashPassword(password.trim());
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);

        boolean created = userRepository.createUser(cleanUsername, hash, timestamp);
        if (created) {
            logger.info("[ĐĂNG KÝ] Tài khoản '{}' đã tạo thành công", cleanUsername);
            return ApiResponse.ok("Tài khoản '" + cleanUsername + "' đã tạo!");
        } else {
            return ApiResponse.error("Lỗi tạo tài khoản");
        }
    }

    public AuthResponse login(String username, String password) {
        ValidationService.ValidationResult userVal = validationService.validateUsername(username);
        if (!userVal.isValid()) {
            return AuthResponse.error(userVal.getMessage());
        }

        ValidationService.ValidationResult passVal = validationService.validatePassword(password);
        if (!passVal.isValid()) {
            return AuthResponse.error(passVal.getMessage());
        }

        String cleanUsername = username.trim();
        Optional<User> userOpt = userRepository.findByUsername(cleanUsername);
        if (userOpt.isEmpty()) {
            return AuthResponse.error("Tài khoản không tồn tại");
        }

        User user = userOpt.get();
        if (user.isDeleted()) {
            return AuthResponse.error("Tài khoản không tồn tại hoặc đã bị xóa");
        }
        if (user.isLocked()) {
            return AuthResponse.error("Tài khoản đã bị khóa bởi quản trị viên");
        }

        String inputHash = hashPassword(password.trim());
        if (!user.getPasswordHash().equals(inputHash)) {
            return AuthResponse.error("Sai mật khẩu");
        }

        String token = tokenService.generateToken(cleanUsername, user.getRole());
        logger.info("[ĐĂNG NHẬP] Người dùng '{}' đăng nhập thành công (Role: {})", cleanUsername, user.getRole());
        return AuthResponse.success(token, cleanUsername, "Chào mừng " + cleanUsername + "!");
    }

    public AuthResponse adminLogin(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return AuthResponse.error("Tên tài khoản và mật khẩu admin không được để trống");
        }

        String cleanUser = username.trim();
        String cleanPass = password.trim();

        // 1. Kiểm tra tài khoản admin cấu hình qua môi trường / application.properties
        if (cleanUser.equalsIgnoreCase(envAdminUsername) && cleanPass.equals(envAdminPassword)) {
            String token = tokenService.generateToken(envAdminUsername, "admin");
            logger.info("[ADMIN LOGIN] Quản trị viên hệ thống '{}' đăng nhập thành công", envAdminUsername);
            return AuthResponse.success(token, envAdminUsername, "Đăng nhập Admin thành công");
        }

        // 2. Kiểm tra tài khoản trong database có role là 'admin' hoặc 'moderator'
        Optional<User> userOpt = userRepository.findByUsername(cleanUser);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.isDeleted() && !user.isLocked()) {
                String role = user.getRole();
                if ("admin".equalsIgnoreCase(role) || "moderator".equalsIgnoreCase(role)) {
                    String inputHash = hashPassword(cleanPass);
                    if (user.getPasswordHash().equals(inputHash)) {
                        String token = tokenService.generateToken(user.getUsername(), role.toLowerCase());
                        logger.info("[ADMIN LOGIN] User '{}' có quyền '{}' đăng nhập admin thành công", user.getUsername(), role);
                        return AuthResponse.success(token, user.getUsername(), "Đăng nhập thành công với quyền " + role);
                    }
                }
            }
        }

        logger.warn("[ADMIN LOGIN THẤT BẠI] Sai thông tin đăng nhập cho user '{}'", cleanUser);
        return AuthResponse.error("Tài khoản hoặc mật khẩu quản trị viên không chính xác");
    }

    public ApiResponse changePassword(String username, String oldPassword, String newPassword) {
        if (username == null || username.trim().isEmpty()) {
            return ApiResponse.error("Phiên đăng nhập không hợp lệ");
        }

        ValidationService.ValidationResult passVal = validationService.validatePassword(newPassword);
        if (!passVal.isValid()) {
            return ApiResponse.error(passVal.getMessage());
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            return ApiResponse.error("Tài khoản không tồn tại");
        }

        User user = userOpt.get();
        String oldHash = hashPassword(oldPassword.trim());
        if (!user.getPasswordHash().equals(oldHash)) {
            return ApiResponse.error("Sai mật khẩu cũ");
        }

        String newHash = hashPassword(newPassword.trim());
        boolean updated = userRepository.updatePassword(username.trim(), newHash);
        if (updated) {
            logger.info("[ĐỔI PASS] Người dùng '{}' đã đổi mật khẩu thành công", username);
            return ApiResponse.ok("Đổi mật khẩu thành công!");
        } else {
            return ApiResponse.error("Không thể cập nhật mật khẩu");
        }
    }
}
