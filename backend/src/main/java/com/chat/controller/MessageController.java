package com.chat.controller;

import com.chat.model.ChatMessage;
import com.chat.repository.MessageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository messageRepository;
    private final com.chat.security.TokenService tokenService;

    public MessageController(MessageRepository messageRepository, com.chat.security.TokenService tokenService) {
        this.messageRepository = messageRepository;
        this.tokenService = tokenService;
    }

    @GetMapping("/public/history")
    public ResponseEntity<List<ChatMessage>> getPublicHistory(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        if (limit <= 0 || limit > 200) {
            limit = 50;
        }
        List<ChatMessage> history = messageRepository.getPublicHistory(limit);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/private/{target}/history")
    public ResponseEntity<?> getPrivateHistory(
            @PathVariable("target") String target,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(java.util.Collections.singletonMap("message", "Chưa đăng nhập"));
        }

        String token = authHeader.substring(7);
        if (!tokenService.validateToken(token)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(java.util.Collections.singletonMap("message", "Token không hợp lệ"));
        }

        String username = tokenService.getUsernameFromToken(token);
        if (username == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(java.util.Collections.singletonMap("message", "Token không hợp lệ"));
        }

        if (limit <= 0 || limit > 200) {
            limit = 50;
        }

        List<ChatMessage> history = messageRepository.getPrivateHistory(username, target, limit);
        return ResponseEntity.ok(history);
    }
}
