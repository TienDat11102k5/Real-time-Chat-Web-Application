package com.chat.repository;

import com.chat.model.ChatMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class MessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public MessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public synchronized boolean savePublicMessage(String username, String message, String timestamp) {
        try {
            String sql = "INSERT INTO public_messages (username, message, timestamp) VALUES (?, ?, ?)";
            int rows = jdbcTemplate.update(sql, username, message, timestamp);
            return rows > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized boolean savePrivateMessage(String sender, String receiver, String message, String timestamp) {
        try {
            String sql = "INSERT INTO private_messages (sender, receiver, message, timestamp) VALUES (?, ?, ?, ?)";
            int rows = jdbcTemplate.update(sql, sender, receiver, message, timestamp);
            return rows > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public List<ChatMessage> getPublicHistory(int limit) {
        String sql = "SELECT id, username, message, timestamp FROM public_messages ORDER BY id DESC LIMIT ?";
        List<ChatMessage> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            ChatMessage msg = new ChatMessage();
            msg.setId(rs.getLong("id"));
            msg.setSender(rs.getString("username"));
            msg.setReceiver(null);
            msg.setMessage(rs.getString("message"));
            msg.setTimestamp(rs.getString("timestamp"));
            msg.setRoom("public");
            return msg;
        }, limit);

        // Đảo ngược lại theo thứ tự thời gian tăng dần như get_history() trong Server.py
        Collections.reverse(list);
        return list;
    }

    public List<ChatMessage> getPrivateHistory(String user1, String user2, int limit) {
        String sql = "SELECT id, sender, receiver, message, timestamp FROM private_messages " +
                "WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) " +
                "ORDER BY id DESC LIMIT ?";
        List<ChatMessage> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            ChatMessage msg = new ChatMessage();
            msg.setId(rs.getLong("id"));
            msg.setSender(rs.getString("sender"));
            msg.setReceiver(rs.getString("receiver"));
            msg.setMessage(rs.getString("message"));
            msg.setTimestamp(rs.getString("timestamp"));
            msg.setRoom("private");
            return msg;
        }, user1, user2, user2, user1, limit);

        // Đảo ngược lại theo thứ tự thời gian tăng dần như get_history() trong Server.py
        Collections.reverse(list);
        return list;
    }

    public int countPublicMessages() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public_messages", Integer.class);
        return count != null ? count : 0;
    }

    public int countPrivateMessages() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM private_messages", Integer.class);
        return count != null ? count : 0;
    }

    public int countPublicMessagesByUser(String username) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public_messages WHERE username = ?", Integer.class, username);
        return count != null ? count : 0;
    }

    public int countPrivateMessagesByUser(String username) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM private_messages WHERE sender = ? OR receiver = ?", Integer.class, username, username);
        return count != null ? count : 0;
    }

    public List<ChatMessage> getUserRecentMessages(String username, int limit) {
        if (limit <= 0) limit = 50;
        String sql = "SELECT id, username AS sender, NULL AS receiver, message, timestamp, 'public' AS room FROM public_messages WHERE username = ? " +
                "UNION ALL " +
                "SELECT id, sender, receiver, message, timestamp, 'private' AS room FROM private_messages WHERE sender = ? OR receiver = ? " +
                "ORDER BY timestamp DESC LIMIT ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ChatMessage msg = new ChatMessage();
            msg.setId(rs.getLong("id"));
            msg.setSender(rs.getString("sender"));
            msg.setReceiver(rs.getString("receiver"));
            msg.setMessage(rs.getString("message"));
            msg.setTimestamp(rs.getString("timestamp"));
            msg.setRoom(rs.getString("room"));
            return msg;
        }, username, username, username, limit);
    }
}
