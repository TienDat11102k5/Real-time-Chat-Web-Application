package com.chat;

import com.chat.model.ChatMessage;
import com.chat.repository.MessageRepository;
import com.chat.security.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketPublicChatTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MessageRepository messageRepository;

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
        private final CountDownLatch latch;

        public TestWsHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            receivedMessages.add(message.getPayload());
            if (latch != null) {
                latch.countDown();
            }
        }

        public List<String> getReceivedMessages() {
            return receivedMessages;
        }
    }

    @Test
    public void testWebSocketPublicChatFlow() throws Exception {
        String alice = "alice_ws_" + System.currentTimeMillis();
        String bob = "bob_ws_" + System.currentTimeMillis();

        String tokenAlice = tokenService.generateToken(alice);
        String tokenBob = tokenService.generateToken(bob);

        StandardWebSocketClient client = new StandardWebSocketClient();

        // 1. Kiểm tra từ chối khi không có token hợp lệ (HTTP Handshake 401)
        Assertions.assertThrows(Exception.class, () -> {
            client.execute(
                    new TextWebSocketHandler() {},
                    "ws://localhost:" + port + "/ws?token=invalid_token"
            ).get(5, TimeUnit.SECONDS);
        }, "Kết nối với token không hợp lệ phải bị từ chối ở handshake");

        // 2. Alice kết nối WebSocket
        CountDownLatch aliceLatch = new CountDownLatch(1);
        TestWsHandler aliceHandler = new TestWsHandler(aliceLatch);
        WebSocketSession aliceSession = client.execute(
                aliceHandler, "ws://localhost:" + port + "/ws?token=" + tokenAlice).get(5, TimeUnit.SECONDS);
        Assertions.assertTrue(aliceSession.isOpen(), "Alice phải kết nối thành công");

        aliceLatch.await(3, TimeUnit.SECONDS);
        Assertions.assertFalse(aliceHandler.getReceivedMessages().isEmpty());

        // 3. Bob kết nối WebSocket
        CountDownLatch bobLatch = new CountDownLatch(1);
        TestWsHandler bobHandler = new TestWsHandler(bobLatch);
        WebSocketSession bobSession = client.execute(
                bobHandler, "ws://localhost:" + port + "/ws?token=" + tokenBob).get(5, TimeUnit.SECONDS);
        Assertions.assertTrue(bobSession.isOpen(), "Bob phải kết nối thành công");

        bobLatch.await(3, TimeUnit.SECONDS);

        // 4. Alice gửi tin nhắn phòng chung
        CountDownLatch broadcastLatch = new CountDownLatch(2);
        aliceHandler.latch.countDown(); // Reset or re-target latch

        String publicChatPayload = "{\"type\":\"public_message\",\"message\":\"Chào Bob và cả phòng!\"}";
        aliceSession.sendMessage(new TextMessage(publicChatPayload));

        Thread.sleep(1000);

        // Kiểm tra Bob nhận được tin nhắn phòng chung của Alice
        boolean bobReceived = bobHandler.getReceivedMessages().stream().anyMatch(msg -> {
            try {
                JsonNode node = objectMapper.readTree(msg);
                return "message".equals(node.path("type").asText())
                        && "public".equals(node.path("room").asText())
                        && alice.equals(node.path("sender").asText())
                        && "Chào Bob và cả phòng!".equals(node.path("message").asText());
            } catch (Exception e) {
                return false;
            }
        });
        Assertions.assertTrue(bobReceived, "Bob phải nhận được tin nhắn phòng chung từ Alice");

        // 5. Kiểm tra REST API lịch sử phòng chung GET /api/messages/public/history
        ResponseEntity<List<ChatMessage>> historyRes = restTemplate.exchange(
                "http://localhost:" + port + "/api/messages/public/history",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ChatMessage>>() {}
        );
        Assertions.assertNotNull(historyRes.getBody());
        boolean foundInHistory = historyRes.getBody().stream()
                .anyMatch(m -> "Chào Bob và cả phòng!".equals(m.getMessage()));
        Assertions.assertTrue(foundInHistory, "Tin nhắn phải được lưu trong SQLite và lấy ra qua REST API");

        // 6. Kiểm tra REST API online users GET /api/users/online
        ResponseEntity<Map> onlineRes = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/users/online", Map.class);
        Assertions.assertNotNull(onlineRes.getBody());
        Assertions.assertTrue((Boolean) onlineRes.getBody().get("ok"));
        Assertions.assertTrue((Integer) onlineRes.getBody().get("total") >= 2);

        // 7. Kiểm tra REST API server limits GET /api/server/limits
        ResponseEntity<Map> limitsRes = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/server/limits", Map.class);
        Assertions.assertNotNull(limitsRes.getBody());
        Assertions.assertEquals(5, limitsRes.getBody().get("maxClients"));
        Assertions.assertEquals(500, limitsRes.getBody().get("maxMessageLength"));

        // 8. Bob ngắt kết nối -> Alice nhận được thông báo Bob rời phòng
        bobSession.close();
        Thread.sleep(1000);

        boolean aliceReceivedLeave = aliceHandler.getReceivedMessages().stream().anyMatch(msg -> {
            try {
                JsonNode node = objectMapper.readTree(msg);
                return "system".equals(node.path("type").asText())
                        && node.path("message").asText().contains(bob);
            } catch (Exception e) {
                return false;
            }
        });
        Assertions.assertTrue(aliceReceivedLeave, "Alice phải nhận được thông báo Bob đã rời phòng chung");

        aliceSession.close();
    }

    @Test
    public void testMax5ClientsEnforced() throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        List<WebSocketSession> sessions = new java.util.ArrayList<>();

        // Kết nối tối đa 5 clients
        for (int i = 1; i <= 5; i++) {
            String uname = "client_limit_" + i + "_" + System.currentTimeMillis();
            String token = tokenService.generateToken(uname);
            WebSocketSession session = client.execute(
                    new TextWebSocketHandler() {},
                    "ws://localhost:" + port + "/ws?token=" + token
            ).get(5, TimeUnit.SECONDS);
            Assertions.assertTrue(session.isOpen(), "Client " + i + " phải kết nối được");
            sessions.add(session);
        }

        // Client thứ 6 kết nối -> Bắt buộc bị từ chối
        String client6 = "client_limit_6_" + System.currentTimeMillis();
        String token6 = tokenService.generateToken(client6);

        Assertions.assertThrows(Exception.class, () -> {
            client.execute(
                    new TextWebSocketHandler() {},
                    "ws://localhost:" + port + "/ws?token=" + token6
            ).get(5, TimeUnit.SECONDS);
        }, "Client thứ 6 bắt buộc phải bị từ chối vì đã đạt giới hạn 5 client");

        // Đóng các session
        for (WebSocketSession s : sessions) {
            try { s.close(); } catch (Exception ignored) {}
        }
    }

    @Test
    public void testMessageLengthLimit() throws Exception {
        String testUser = "user_len_" + System.currentTimeMillis();
        String token = tokenService.generateToken(testUser);

        StandardWebSocketClient client = new StandardWebSocketClient();
        TestWsHandler handler = new TestWsHandler(null);
        WebSocketSession session = client.execute(
                handler, "ws://localhost:" + port + "/ws?token=" + token
        ).get(5, TimeUnit.SECONDS);

        // Gửi tin nhắn 501 ký tự -> Bị từ chối
        String longMessage = "A".repeat(501);
        session.sendMessage(new TextMessage("{\"type\":\"public_message\",\"message\":\"" + longMessage + "\"}"));
        Thread.sleep(500);

        boolean gotLengthError = handler.getReceivedMessages().stream().anyMatch(m -> m.contains("không được vượt quá 500"));
        Assertions.assertTrue(gotLengthError, "Tin nhắn vượt quá 500 ký tự phải bị server từ chối");

        // Gửi tin nhắn đúng 500 ký tự -> Thành công
        String valid500Message = "B".repeat(500);
        session.sendMessage(new TextMessage("{\"type\":\"public_message\",\"message\":\"" + valid500Message + "\"}"));
        Thread.sleep(500);

        boolean got500Success = handler.getReceivedMessages().stream().anyMatch(m -> m.contains(valid500Message));
        Assertions.assertTrue(got500Success, "Tin nhắn 500 ký tự hợp lệ phải được server chấp nhận");

        session.close();
    }
}
