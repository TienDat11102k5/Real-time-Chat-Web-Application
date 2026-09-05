package com.chat;

import com.chat.repository.AuditLogRepository;
import com.chat.repository.UserRepository;
import com.chat.security.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Admin Login: Thành công với thông tin đúng, thất bại với sai password")
    void testAdminLogin() throws Exception {
        // 1. Sai mật khẩu
        Map<String, String> badCreds = Map.of("username", "admin", "password", "wrongpass");
        mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badCreds)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));

        // 2. Đúng mật khẩu mặc định (admin / admin123456)
        Map<String, String> goodCreds = Map.of("username", "admin", "password", "admin123456");
        MvcResult res = mockMvc.perform(post("/api/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goodCreds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        String token = node.get("token").asText();
        assertNotNull(token);
        assertEquals("admin", tokenService.getRoleFromToken(token));
    }

    @Test
    @DisplayName("Admin Security: 401 nếu thiếu token, 403 nếu user thường, 200 nếu admin")
    void testAdminSecurity() throws Exception {
        // 1. Không có token -> 401 Unauthorized
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // 2. Token của user thường -> 403 Forbidden
        String userToken = tokenService.generateToken("regular_user", "user");
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // 3. Token của admin -> 200 OK
        String adminToken = tokenService.generateToken("admin", "admin");
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.max_clients").value(5))
                .andExpect(jsonPath("$.database").value("ok"));
    }

    @Test
    @DisplayName("Admin Account Operations: Khóa, Mở khóa, Đặt lại mật khẩu, Audit Log")
    void testAccountOperationsAndAudit() throws Exception {
        String testUser = "admintest_" + System.currentTimeMillis();
        userRepository.createUser(testUser, "hash_dummy", "2026-09-05 12:00:00");

        String adminToken = tokenService.generateToken("admin", "admin");

        // 1. Lock user
        mockMvc.perform(post("/api/admin/accounts/" + testUser + "/lock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        assertTrue(userRepository.findByUsername(testUser).get().isLocked());

        // 2. Unlock user
        mockMvc.perform(post("/api/admin/accounts/" + testUser + "/unlock")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        assertFalse(userRepository.findByUsername(testUser).get().isLocked());

        // 3. Reset password
        Map<String, String> resetBody = Map.of("new_password", "newsecret123");
        mockMvc.perform(post("/api/admin/accounts/" + testUser + "/reset-password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // 4. Kiểm tra Audit logs
        mockMvc.perform(get("/api/admin/logs?limit=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
