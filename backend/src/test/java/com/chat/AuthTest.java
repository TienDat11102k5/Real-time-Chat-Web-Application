package com.chat;

import com.chat.dto.ApiResponse;
import com.chat.dto.AuthRequest;
import com.chat.dto.AuthResponse;
import com.chat.dto.ChangePasswordRequest;
import com.chat.service.AuthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthService authService;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/auth";
    }

    @Test
    public void testSha256CompatibilityWithPython() {
        // Test kiểm tra hash SHA-256 của Java khớp tuyệt đối với python hashlib.sha256("123456".encode()).hexdigest()
        // Python: hashlib.sha256("123456".encode()).hexdigest() -> 8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92
        String hash = AuthService.hashPassword("123456");
        Assertions.assertEquals("8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92", hash);
    }

    @Test
    public void testAuthRestApiFlow() {
        String testUser = "user_" + System.currentTimeMillis();
        String testPass = "password123";

        // 1. Đăng ký thành công
        AuthRequest regReq = new AuthRequest(testUser, testPass);
        ResponseEntity<ApiResponse> regRes = restTemplate.postForEntity(
                getBaseUrl() + "/register", regReq, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, regRes.getStatusCode());
        Assertions.assertNotNull(regRes.getBody());
        Assertions.assertTrue(regRes.getBody().isOk());
        Assertions.assertTrue(regRes.getBody().getMessage().contains("đã tạo"));

        // 2. Đăng ký trùng tên tài khoản -> Bị từ chối
        ResponseEntity<ApiResponse> dupRes = restTemplate.postForEntity(
                getBaseUrl() + "/register", regReq, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, dupRes.getStatusCode());
        Assertions.assertNotNull(dupRes.getBody());
        Assertions.assertFalse(dupRes.getBody().isOk());
        Assertions.assertEquals("Tên tài khoản đã tồn tại", dupRes.getBody().getMessage());

        // 3. Đăng ký tên chứa từ khóa hệ thống -> Bị từ chối
        AuthRequest adminReq = new AuthRequest("ADMIN", "password123");
        ResponseEntity<ApiResponse> adminRes = restTemplate.postForEntity(
                getBaseUrl() + "/register", adminReq, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, adminRes.getStatusCode());

        // 4. Đăng nhập sai mật khẩu -> Bị từ chối
        AuthRequest wrongPassReq = new AuthRequest(testUser, "wrongPassword");
        ResponseEntity<AuthResponse> wrongPassRes = restTemplate.postForEntity(
                getBaseUrl() + "/login", wrongPassReq, AuthResponse.class);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, wrongPassRes.getStatusCode());
        Assertions.assertNotNull(wrongPassRes.getBody());
        Assertions.assertFalse(wrongPassRes.getBody().isOk());
        Assertions.assertEquals("Sai mật khẩu", wrongPassRes.getBody().getMessage());

        // 5. Đăng nhập đúng mật khẩu -> Thành công, nhận token
        ResponseEntity<AuthResponse> loginRes = restTemplate.postForEntity(
                getBaseUrl() + "/login", regReq, AuthResponse.class);
        Assertions.assertEquals(HttpStatus.OK, loginRes.getStatusCode());
        Assertions.assertNotNull(loginRes.getBody());
        Assertions.assertTrue(loginRes.getBody().isOk());
        Assertions.assertNotNull(loginRes.getBody().getToken());
        Assertions.assertEquals(testUser, loginRes.getBody().getUsername());
        String token = loginRes.getBody().getToken();

        // 6. Đổi mật khẩu với mật khẩu cũ sai -> Bị từ chối
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<ChangePasswordRequest> wrongOldPassEntity = new HttpEntity<>(
                new ChangePasswordRequest("wrongOldPass", "newPassword456"), headers);
        ResponseEntity<ApiResponse> wrongOldPassRes = restTemplate.postForEntity(
                getBaseUrl() + "/change-password", wrongOldPassEntity, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, wrongOldPassRes.getStatusCode());
        Assertions.assertEquals("Sai mật khẩu cũ", wrongOldPassRes.getBody().getMessage());

        // 7. Đổi mật khẩu thành công
        String newPass = "newPassword456";
        HttpEntity<ChangePasswordRequest> validChangePassEntity = new HttpEntity<>(
                new ChangePasswordRequest(testPass, newPass), headers);
        ResponseEntity<ApiResponse> changePassRes = restTemplate.postForEntity(
                getBaseUrl() + "/change-password", validChangePassEntity, ApiResponse.class);
        Assertions.assertEquals(HttpStatus.OK, changePassRes.getStatusCode());
        Assertions.assertTrue(changePassRes.getBody().isOk());
        Assertions.assertEquals("Đổi mật khẩu thành công!", changePassRes.getBody().getMessage());

        // 8. Đăng nhập bằng mật khẩu mới -> Thành công
        AuthRequest newLoginReq = new AuthRequest(testUser, newPass);
        ResponseEntity<AuthResponse> newLoginRes = restTemplate.postForEntity(
                getBaseUrl() + "/login", newLoginReq, AuthResponse.class);
        Assertions.assertEquals(HttpStatus.OK, newLoginRes.getStatusCode());
        Assertions.assertTrue(newLoginRes.getBody().isOk());
    }
}
