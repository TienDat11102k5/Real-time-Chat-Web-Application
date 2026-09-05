package com.chat;

import com.chat.model.ChatMessage;
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
import org.springframework.http.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketPrivateChatTest {

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
    }

    @Test
    public void testPrivateChatCompleteFlow() throws Exception {
        String alice = "alice_priv_" + System.currentTimeMillis();
        String bob = "bob_priv_" + System.currentTimeMillis();
        String charlie = "charlie_priv_" + System.currentTimeMillis();

        String tokenAlice = tokenService.generateToken(alice);
        String tokenBob = tokenService.generateToken(bob);
        String tokenCharlie = tokenService.generateToken(charlie);

        StandardWebSocketClient client = new StandardWebSocketClient();

        TestWsHandler aliceHandler = new TestWsHandler();
        WebSocketSession aliceSession = client.execute(
                aliceHandler, "ws://localhost:" + port + "/ws?token=" + tokenAlice).get(5, TimeUnit.SECONDS);

        TestWsHandler bobHandler = new TestWsHandler();
        WebSocketSession bobSession = client.execute(
                bobHandler, "ws://localhost:" + port + "/ws?token=" + tokenBob).get(5, TimeUnit.SECONDS);

        TestWsHandler charlieHandler = new TestWsHandler();
        WebSocketSession charlieSession = client.execute(
                charlieHandler, "ws://localhost:" + port + "/ws?token=" + tokenCharlie).get(5, TimeUnit.SECONDS);

        Thread.sleep(500);

        // 1. Alice gửi yêu cầu cho chính mình -> Bị báo lỗi
        aliceSession.sendMessage(new TextMessage("{\"type\":\"private_request\",\"target\":\"" + alice + "\",\"message\":\"hello\"}"));
        Thread.sleep(300);
        boolean selfError = aliceHandler.getReceivedMessages().stream().anyMatch(m -> m.contains("chính mình"));
        Assertions.assertTrue(selfError, "Không thể gửi yêu cầu chat riêng cho chính mình");

        // 2. Alice gửi yêu cầu tới Bob, Bob từ chối (Decline)
        aliceSession.sendMessage(new TextMessage("{\"type\":\"private_request\",\"target\":\"" + bob + "\",\"message\":\"Chat rieng 1 nhe\"}"));
        Thread.sleep(500);

        boolean bobGotRequest1 = bobHandler.getReceivedMessages().stream().anyMatch(m -> m.contains("Chat rieng 1 nhe"));
        Assertions.assertTrue(bobGotRequest1, "Bob phải nhận được yêu cầu chat riêng từ Alice");

        // Bob gửi private_decline
        bobSession.sendMessage(new TextMessage("{\"type\":\"private_decline\",\"from\":\"" + alice + "\"}"));
        Thread.sleep(500);

        boolean aliceGotDecline = aliceHandler.getReceivedMessages().stream().anyMatch(m -> m.contains("từ chối"));
        Assertions.assertTrue(aliceGotDecline, "Alice phải nhận được thông báo Bob từ chối");

        // 3. Alice gửi lại yêu cầu tới Bob, Bob chấp nhận (Accept)
        aliceSession.sendMessage(new TextMessage("{\"type\":\"private_request\",\"target\":\"" + bob + "\",\"message\":\"Chat rieng 2 nhe!\"}"));
        Thread.sleep(500);

        bobSession.sendMessage(new TextMessage("{\"type\":\"private_accept\",\"from\":\"" + alice + "\"}"));
        Thread.sleep(500);

        // Kiểm tra cả 2 đều nhận được chuyển trạng thái phòng room_state = private
        boolean aliceInPrivate = aliceHandler.getReceivedMessages().stream().anyMatch(m -> {
            try {
                JsonNode n = objectMapper.readTree(m);
                return "room_state".equals(n.path("type").asText()) && "private".equals(n.path("room").asText());
            } catch (Exception e) {
                return false;
            }
        });
        boolean bobInPrivate = bobHandler.getReceivedMessages().stream().anyMatch(m -> {
            try {
                JsonNode n = objectMapper.readTree(m);
                return "room_state".equals(n.path("type").asText()) && "private".equals(n.path("room").asText());
            } catch (Exception e) {
                return false;
            }
        });
        Assertions.assertTrue(aliceInPrivate && bobInPrivate, "Cả 2 user phải chuyển sang trạng thái phòng riêng");

        // 4. Alice gửi tin nhắn riêng cho Bob
        aliceSession.sendMessage(new TextMessage("{\"type\":\"private_message\",\"message\":\"Bi mat giua Alice va Bob!\"}"));
        Thread.sleep(500);

        boolean bobReceivedPrivateMsg = bobHandler.getReceivedMessages().stream().anyMatch(m -> m.contains("Bi mat giua Alice va Bob!"));
        Assertions.assertTrue(bobReceivedPrivateMsg, "Bob phải nhận được tin nhắn riêng từ Alice");

        // Kiểm tra Charlie ở phòng chung HOÀN TOÀN KHÔNG nhận được tin nhắn riêng này!
        boolean charlieReceivedPrivateMsg = charlieHandler.getReceivedMessages().stream().anyMatch(m -> m.contains("Bi mat giua Alice va Bob!"));
        Assertions.assertFalse(charlieReceivedPrivateMsg, "Charlie không được nhận tin nhắn riêng của Alice và Bob");

        // 5. Kiểm tra REST API lấy lịch sử chat riêng GET /api/messages/private/{bob}/history
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenAlice);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<List<ChatMessage>> privHistRes = restTemplate.exchange(
                "http://localhost:" + port + "/api/messages/private/" + bob + "/history",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<ChatMessage>>() {}
        );
        Assertions.assertEquals(HttpStatus.OK, privHistRes.getStatusCode());
        Assertions.assertNotNull(privHistRes.getBody());
        boolean foundPrivateInHistory = privHistRes.getBody().stream()
                .anyMatch(m -> "Bi mat giua Alice va Bob!".equals(m.getMessage()));
        Assertions.assertTrue(foundPrivateInHistory, "Lịch sử tin nhắn riêng phải được lưu trong SQLite");

        // 6. Alice quay lại phòng chung (back_to_public)
        aliceSession.sendMessage(new TextMessage("{\"type\":\"back_to_public\"}"));
        Thread.sleep(500);

        boolean aliceBackPublic = aliceHandler.getReceivedMessages().stream().anyMatch(m -> {
            try {
                JsonNode n = objectMapper.readTree(m);
                return "room_state".equals(n.path("type").asText()) && "public".equals(n.path("room").asText());
            } catch (Exception e) {
                return false;
            }
        });
        Assertions.assertTrue(aliceBackPublic, "Alice phải quay lại phòng chung");

        aliceSession.close();
        bobSession.close();
        charlieSession.close();
    }
}
