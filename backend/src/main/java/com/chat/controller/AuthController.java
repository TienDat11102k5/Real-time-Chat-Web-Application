package com.chat.controller;

import com.chat.dto.ApiResponse;
import com.chat.dto.AuthRequest;
import com.chat.dto.AuthResponse;
import com.chat.dto.ChangePasswordRequest;
import com.chat.security.TokenService;
import com.chat.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody AuthRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Dữ liệu yêu cầu không hợp lệ"));
        }
        ApiResponse response = authService.register(request.getUsername(), request.getPassword());
        if (response.isOk()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(AuthResponse.error("Dữ liệu yêu cầu không hợp lệ"));
        }
        AuthResponse response = authService.login(request.getUsername(), request.getPassword());
        if (response.isOk()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ChangePasswordRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập"));
        }

        String token = authHeader.substring(7);
        if (!tokenService.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Phiên đăng nhập hết hạn hoặc không hợp lệ"));
        }

        String username = tokenService.getUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token không hợp lệ"));
        }

        if (request == null || request.getOldPassword() == null || request.getNewPassword() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng nhập đầy đủ mật khẩu cũ và mới"));
        }

        ApiResponse response = authService.changePassword(username, request.getOldPassword(), request.getNewPassword());
        if (response.isOk()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Map<String, Object> res = new HashMap<>();
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            res.put("ok", false);
            res.put("message", "Chưa đăng nhập");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }

        String token = authHeader.substring(7);
        if (!tokenService.validateToken(token)) {
            res.put("ok", false);
            res.put("message", "Token không hợp lệ");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }

        String username = tokenService.getUsernameFromToken(token);
        res.put("ok", true);
        res.put("username", username);
        return ResponseEntity.ok(res);
    }
}
