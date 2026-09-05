package com.chat.controller;

import com.chat.service.ValidationService;
import com.chat.websocket.ConnectionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/server")
public class ServerController {

    private final ConnectionManager connectionManager;

    public ServerController(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @GetMapping("/limits")
    public ResponseEntity<Map<String, Object>> getLimits() {
        Map<String, Object> limits = new HashMap<>();
        limits.put("maxClients", ConnectionManager.MAX_CLIENTS);
        limits.put("currentClients", connectionManager.getClientCount());
        limits.put("maxMessageLength", ValidationService.MAX_MESSAGE_LENGTH);
        limits.put("minUsernameLength", ValidationService.MIN_USERNAME_LENGTH);
        limits.put("maxUsernameLength", ValidationService.MAX_USERNAME_LENGTH);
        limits.put("minPasswordLength", ValidationService.MIN_PASSWORD_LENGTH);
        limits.put("maxPasswordLength", ValidationService.MAX_PASSWORD_LENGTH);
        limits.put("requestTimeoutSeconds", 60);
        return ResponseEntity.ok(limits);
    }
}
