package com.chat.controller;

import com.chat.websocket.ConnectionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ConnectionManager connectionManager;

    public UserController(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @GetMapping("/online")
    public ResponseEntity<Map<String, Object>> getOnlineUsers() {
        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        res.put("total", connectionManager.getClientCount());
        res.put("maxClients", ConnectionManager.MAX_CLIENTS);
        res.put("users", connectionManager.getOnlineUsersDetails());
        return ResponseEntity.ok(res);
    }
}
