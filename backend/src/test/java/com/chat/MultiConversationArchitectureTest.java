package com.chat;

import com.chat.security.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MultiConversationArchitectureTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private com.chat.websocket.ConnectionManager connectionManager;

    @org.junit.jupiter.api.BeforeEach
    @org.junit.jupiter.api.AfterEach
    public void cleanup() {
        if (connectionManager != null) {
            connectionManager.resetForTesting();
        }
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static class TestWsHandler extends TextWebSocketHandler {
        private final List<String> receivedMessages = new CopyOnWriteArrayList<>();

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            receivedMessages.add(message.getPayload());
        }

        public List<String> getReceivedMessages() {
            return receivedMessages;
        }

        public boolean hasMessageContaining(String text) {
            return receivedMessages.stream().anyMatch(m -> m.contains(text));
        }

        public boolean hasMessageType(String type) {
            return receivedMessages.stream().anyMatch(m -> {
                try {
                    JsonNode n = new ObjectMapper().readTree(m);
                    return type.equals(n.path("type").asText());
                } catch (Exception e) {
                    return false;
                }
            });
        }
    }

    @Test
    public void testMultiConversationArchitectureScenario() throws Exception {
        String alice = "alice_arch_" + System.currentTimeMillis();
        String bob = "bob_arch_" + System.currentTimeMillis();
        String charlie = "charlie_arch_" + System.currentTimeMillis();

        String tokenAlice = tokenService.generateToken(alice);
        String tokenBob = tokenService.generateToken(bob);
        String tokenCharlie = tokenService.generateToken(charlie);

        StandardWebSocketClient client = new StandardWebSocketClient();

        // 1. User Alice, Bob, Charlie cùng đăng nhập và kết nối WebSocket
        TestWsHandler aliceHandler = new TestWsHandler();
        WebSocketSession aliceSession = client.execute(
                aliceHandler, "ws://localhost:" + port + "/ws?token=" + tokenAlice).get(5, TimeUnit.SECONDS);

        TestWsHandler bobHandler = new TestWsHandler();
        WebSocketSession bobSession = client.execute(
                bobHandler, "ws://localhost:" + port + "/ws?token=" + tokenBob).get(5, TimeUnit.SECONDS);

        TestWsHandler charlieHandler = new TestWsHandler();
        WebSocketSession charlieSession = client.execute(
                charlieHandler, "ws://localhost:" + port + "/ws?token=" + tokenCharlie).get(5, TimeUnit.SECONDS);

        Thread.sleep(600);

        // Đăng nhập admin để gọi /api/admin/stats
        HttpHeaders adminHeaders = new HttpHeaders();
        HttpEntity<Map<String, String>> loginReq = new HttpEntity<>(Map.of("username", "admin", "password", "admin123456"));
        ResponseEntity<Map> loginResp = restTemplate.postForEntity("http://localhost:" + port + "/api/admin/login", loginReq, Map.class);
        Assertions.assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        String adminToken = (String) loginResp.getBody().get("token");
        adminHeaders.setBearerAuth(adminToken);

        // 2. Kiểm tra GET /api/admin/stats: online_users = 3, public_room_users = 3, private_pairs = 0
        ResponseEntity<Map> stats1 = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/stats",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                Map.class
        );
        Assertions.assertEquals(HttpStatus.OK, stats1.getStatusCode());
        Assertions.assertEquals(3, stats1.getBody().get("online_users"));
        Assertions.assertEquals(3, stats1.getBody().get("public_room_users"));
        Assertions.assertEquals(0, stats1.getBody().get("private_pairs"));

        // 3. Alice gửi private request tới Bob kèm preview "secret chat"
        aliceSession.sendMessage(new TextMessage("{\"type\":\"private_request\",\"target\":\"" + bob + "\",\"message\":\"secret chat\"}"));
        Thread.sleep(500);

        Assertions.assertTrue(bobHandler.hasMessageContaining("secret chat"), "Bob phải nhận được private_request từ Alice");

        // 4. Bob accept private request từ Alice
        bobSession.sendMessage(new TextMessage("{\"type\":\"private_accept\",\"from\":\"" + alice + "\"}"));
        Thread.sleep(500);

        // Server gửi private_session_started cho cả Alice và Bob
        Assertions.assertTrue(aliceHandler.hasMessageType("private_session_started"), "Alice phải nhận được private_session_started");
        Assertions.assertTrue(bobHandler.hasMessageType("private_session_started"), "Bob phải nhận được private_session_started");

        // Không ai nhận được tin nhắn "{user} đã rời phòng chung"
        boolean charlieSawLeave = charlieHandler.getReceivedMessages().stream().anyMatch(m -> m.contains("đã rời phòng chung"));
        Assertions.assertFalse(charlieSawLeave, "Không được broadcast rời phòng chung khi user vào private chat");

        // 5. Kiểm tra GET /api/admin/stats: online_users = 3, public_room_users = 3, private_pairs = 1
        ResponseEntity<Map> stats2 = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/stats",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                Map.class
        );
        Assertions.assertEquals(3, stats2.getBody().get("online_users"));
        Assertions.assertEquals(3, stats2.getBody().get("public_room_users"));
        Assertions.assertEquals(1, stats2.getBody().get("private_pairs"));

        // 6. Charlie gửi tin nhắn vào phòng chung
        charlieSession.sendMessage(new TextMessage("{\"type\":\"public_message\",\"message\":\"Hello everybody from Charlie!\"}"));
        Thread.sleep(500);

        // CẢ ALICE VÀ BOB (dù đang trong private chat session) VẪN PHẢI NHẬN ĐƯỢC PUBLIC MESSAGE!
        Assertions.assertTrue(aliceHandler.hasMessageContaining("Hello everybody from Charlie!"),
                "Alice PHẢI nhận được public message của Charlie dù đang có private session với Bob");
        Assertions.assertTrue(bobHandler.hasMessageContaining("Hello everybody from Charlie!"),
                "Bob PHẢI nhận được public message của Charlie dù đang có private session với Alice");
        Assertions.assertTrue(charlieHandler.hasMessageContaining("Hello everybody from Charlie!"),
                "Charlie phải nhận được tin nhắn phòng chung");

        // 7. Alice gửi tin nhắn riêng tới Bob
        aliceSession.sendMessage(new TextMessage("{\"type\":\"private_message\",\"target\":\"" + bob + "\",\"message\":\"Tin nhan tuyet mat Alice gui Bob\"}"));
        Thread.sleep(500);

        // Bob và Alice nhận được, Charlie KHÔNG nhận được
        Assertions.assertTrue(bobHandler.hasMessageContaining("Tin nhan tuyet mat Alice gui Bob"), "Bob phải nhận được tin nhắn riêng");
        Assertions.assertTrue(aliceHandler.hasMessageContaining("Tin nhan tuyet mat Alice gui Bob"), "Alice phải nhận được tin nhắn riêng của chính mình");
        Assertions.assertFalse(charlieHandler.hasMessageContaining("Tin nhan tuyet mat Alice gui Bob"), "Charlie KHÔNG ĐƯỢC nhận tin nhắn riêng");

        // 8. Alice gửi action end_private_chat tới Bob
        aliceSession.sendMessage(new TextMessage("{\"type\":\"end_private_chat\",\"target\":\"" + bob + "\"}"));
        Thread.sleep(500);

        // Server gửi private_session_ended cho Alice và Bob
        Assertions.assertTrue(aliceHandler.hasMessageType("private_session_ended"), "Alice phải nhận được private_session_ended");
        Assertions.assertTrue(bobHandler.hasMessageType("private_session_ended"), "Bob phải nhận được private_session_ended");

        // 9. Kiểm tra GET /api/admin/stats: online_users = 3, public_room_users = 3, private_pairs = 0
        ResponseEntity<Map> stats3 = restTemplate.exchange(
                "http://localhost:" + port + "/api/admin/stats",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                Map.class
        );
        Assertions.assertEquals(3, stats3.getBody().get("online_users"));
        Assertions.assertEquals(3, stats3.getBody().get("public_room_users"));
        Assertions.assertEquals(0, stats3.getBody().get("private_pairs"));

        // 10. Charlie gửi lại tin nhắn phòng chung: Cả 3 người đều nhận được bình thường
        charlieSession.sendMessage(new TextMessage("{\"type\":\"public_message\",\"message\":\"Public round 2 after private ended!\"}"));
        Thread.sleep(500);

        Assertions.assertTrue(aliceHandler.hasMessageContaining("Public round 2 after private ended!"), "Alice phải nhận tin chung sau khi đóng private");
        Assertions.assertTrue(bobHandler.hasMessageContaining("Public round 2 after private ended!"), "Bob phải nhận tin chung sau khi đóng private");
        Assertions.assertTrue(charlieHandler.hasMessageContaining("Public round 2 after private ended!"), "Charlie phải nhận tin chung sau khi đóng private");

        aliceSession.close();
        bobSession.close();
        charlieSession.close();
    }

    @Test
    public void testPageReloadPreservesPrivateSession() throws Exception {
        String alice = "alice_reload_" + System.currentTimeMillis();
        String bob = "bob_reload_" + System.currentTimeMillis();

        String tokenAlice = tokenService.generateToken(alice);
        String tokenBob = tokenService.generateToken(bob);

        StandardWebSocketClient client = new StandardWebSocketClient();

        TestWsHandler aliceHandler1 = new TestWsHandler();
        WebSocketSession aliceSession1 = client.execute(
                aliceHandler1, "ws://localhost:" + port + "/ws?token=" + tokenAlice).get(5, TimeUnit.SECONDS);

        TestWsHandler bobHandler = new TestWsHandler();
        WebSocketSession bobSession = client.execute(
                bobHandler, "ws://localhost:" + port + "/ws?token=" + tokenBob).get(5, TimeUnit.SECONDS);

        Thread.sleep(500);

        // 1. Alice yêu cầu chat riêng với Bob và Bob chấp nhận
        aliceSession1.sendMessage(new TextMessage("{\"type\":\"private_request\",\"target\":\"" + bob + "\",\"preview\":\"Hi Bob\"}"));
        Thread.sleep(500);

        bobSession.sendMessage(new TextMessage("{\"type\":\"private_accept\",\"target\":\"" + alice + "\"}"));
        Thread.sleep(500);

        Assertions.assertTrue(aliceHandler1.hasMessageType("private_session_started"), "Alice phải nhận private_session_started ban đầu");
        Assertions.assertTrue(bobHandler.hasMessageType("private_session_started"), "Bob phải nhận private_session_started");

        // 2. Alice reload lại trang (đóng session cũ)
        aliceSession1.close();
        Thread.sleep(500);

        // Bob KHÔNG bị mất session hay nhận thông báo kết thúc phiên
        Assertions.assertFalse(bobHandler.hasMessageType("private_session_ended"), "Bob KHÔNG được nhận private_session_ended khi Alice reload trang");

        // 3. Alice kết nối lại với session mới (sau khi F5 tải lại trang)
        TestWsHandler aliceHandler2 = new TestWsHandler();
        WebSocketSession aliceSession2 = client.execute(
                aliceHandler2, "ws://localhost:" + port + "/ws?token=" + tokenAlice).get(5, TimeUnit.SECONDS);
        Thread.sleep(500);

        // Alice được phục hồi private session tự động từ server
        Assertions.assertTrue(aliceHandler2.hasMessageType("private_session_started"), "Alice phải nhận được private_session_started phục hồi");

        // 4. Alice gửi tin nhắn riêng cho Bob -> Bob nhận được bình thường
        aliceSession2.sendMessage(new TextMessage("{\"type\":\"private_message\",\"target\":\"" + bob + "\",\"message\":\"Alo Bob to da F5 reload trang xong!\"}"));
        Thread.sleep(500);

        Assertions.assertTrue(bobHandler.hasMessageContaining("Alo Bob to da F5 reload trang xong!"), "Bob phải nhận được tin sau khi Alice reload");

        // 5. Bob trả lời Alice -> Alice nhận được bình thường
        bobSession.sendMessage(new TextMessage("{\"type\":\"private_message\",\"target\":\"" + alice + "\",\"message\":\"Chao mung tro lai Alice!\"}"));
        Thread.sleep(500);

        Assertions.assertTrue(aliceHandler2.hasMessageContaining("Chao mung tro lai Alice!"), "Alice phải nhận được tin Bob trả lời");

        // 6. Alice kết thúc chat riêng
        aliceSession2.sendMessage(new TextMessage("{\"type\":\"end_private_chat\",\"target\":\"" + bob + "\"}"));
        Thread.sleep(500);

        Assertions.assertTrue(bobHandler.hasMessageType("private_session_ended"), "Bob nhận private_session_ended khi kết thúc");
        Assertions.assertTrue(aliceHandler2.hasMessageType("private_session_ended"), "Alice nhận private_session_ended khi kết thúc");

        aliceSession2.close();
        bobSession.close();
    }
}
