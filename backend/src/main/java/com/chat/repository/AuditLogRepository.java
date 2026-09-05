package com.chat.repository;

import com.chat.model.AuditLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AuditLogRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AuditLog> rowMapper = new RowMapper<AuditLog>() {
        @Override
        public AuditLog mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AuditLog(
                    rs.getLong("id"),
                    rs.getString("timestamp"),
                    rs.getString("admin_username"),
                    rs.getString("action"),
                    rs.getString("target"),
                    rs.getString("result"),
                    rs.getString("details")
            );
        }
    };

    public synchronized void record(String adminUsername, String action, String target, String result, String details) {
        try {
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String sql = "INSERT INTO audit_logs (timestamp, admin_username, action, target, result, details) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, timestamp, adminUsername, action, target, result, details);
        } catch (Exception ignored) {
        }
    }

    public List<AuditLog> findRecent(int limit, String actionFilter) {
        if (limit <= 0) limit = 100;
        if (limit > 500) limit = 500;

        if (actionFilter != null && !actionFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(actionFilter.trim())) {
            String sql = "SELECT id, timestamp, admin_username, action, target, result, details FROM audit_logs WHERE UPPER(action) = UPPER(?) ORDER BY id DESC LIMIT ?";
            return jdbcTemplate.query(sql, rowMapper, actionFilter.trim(), limit);
        } else {
            String sql = "SELECT id, timestamp, admin_username, action, target, result, details FROM audit_logs ORDER BY id DESC LIMIT ?";
            return jdbcTemplate.query(sql, rowMapper, limit);
        }
    }
}
