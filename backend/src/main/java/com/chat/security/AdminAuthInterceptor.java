package com.chat.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminAuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Cho phép CORS Preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        // Cho phép endpoint đăng nhập admin không cần token
        if (path.endsWith("/api/admin/login")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Thiếu hoặc sai định dạng Authorization Bearer token");
            return false;
        }

        String token = authHeader.substring(7).trim();
        if (!tokenService.validateToken(token)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Phiên đăng nhập admin không hợp lệ hoặc đã hết hạn");
            return false;
        }

        String username = tokenService.getUsernameFromToken(token);
        String role = tokenService.getRoleFromToken(token);

        if (role == null || (!"admin".equalsIgnoreCase(role) && !"moderator".equalsIgnoreCase(role))) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", "Bạn không có quyền truy cập bảng điều khiển quản trị");
            return false;
        }

        // Lưu thông tin vào request attribute để controller dễ sử dụng
        request.setAttribute("adminUsername", username);
        request.setAttribute("adminRole", role.toLowerCase());

        return true;
    }

    private void sendError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> errMap = new HashMap<>();
        errMap.put("status", status);
        errMap.put("error", error);
        errMap.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(errMap));
    }
}
