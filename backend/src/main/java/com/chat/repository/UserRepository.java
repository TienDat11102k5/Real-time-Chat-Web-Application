package com.chat.repository;

import com.chat.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            String role = "user";
            String status = "active";
            String lockedAt = null;
            String deletedAt = null;

            try {
                role = rs.getString("role");
                if (role == null) role = "user";
            } catch (SQLException ignored) {}

            try {
                status = rs.getString("status");
                if (status == null) status = "active";
            } catch (SQLException ignored) {}

            try {
                lockedAt = rs.getString("locked_at");
            } catch (SQLException ignored) {}

            try {
                deletedAt = rs.getString("deleted_at");
            } catch (SQLException ignored) {}

            return new User(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("created_at"),
                    role,
                    status,
                    lockedAt,
                    deletedAt
            );
        }
    };

    public Optional<User> findByUsername(String username) {
        try {
            String sql = "SELECT id, username, password_hash, created_at, role, status, locked_at, deleted_at FROM users WHERE username = ?";
            User user = jdbcTemplate.queryForObject(sql, userRowMapper, username);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    public synchronized boolean createUser(String username, String passwordHash, String createdAt) {
        try {
            String sql = "INSERT INTO users (username, password_hash, created_at, role, status) VALUES (?, ?, ?, 'user', 'active')";
            int rows = jdbcTemplate.update(sql, username, passwordHash, createdAt);
            return rows > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized boolean updatePassword(String username, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        int rows = jdbcTemplate.update(sql, newPasswordHash, username);
        return rows > 0;
    }

    public synchronized boolean lockUser(String username) {
        String now = LocalDateTime.now().format(FORMATTER);
        String sql = "UPDATE users SET status = 'locked', locked_at = ? WHERE username = ?";
        return jdbcTemplate.update(sql, now, username) > 0;
    }

    public synchronized boolean unlockUser(String username) {
        String sql = "UPDATE users SET status = 'active', locked_at = NULL WHERE username = ?";
        return jdbcTemplate.update(sql, username) > 0;
    }

    public synchronized boolean updateRole(String username, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE username = ?";
        return jdbcTemplate.update(sql, newRole, username) > 0;
    }

    public synchronized boolean softDeleteUser(String username) {
        String now = LocalDateTime.now().format(FORMATTER);
        String sql = "UPDATE users SET deleted_at = ? WHERE username = ?";
        return jdbcTemplate.update(sql, now, username) > 0;
    }

    public List<User> findAll() {
        String sql = "SELECT id, username, password_hash, created_at, role, status, locked_at, deleted_at FROM users ORDER BY id ASC";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    public List<User> searchAccounts(String query, int page, int limit) {
        if (page < 1) page = 1;
        if (limit < 1) limit = 10;
        int offset = (page - 1) * limit;

        if (query != null && !query.trim().isEmpty()) {
            String pattern = "%" + query.trim() + "%";
            String sql = "SELECT id, username, password_hash, created_at, role, status, locked_at, deleted_at FROM users WHERE username LIKE ? ORDER BY id DESC LIMIT ? OFFSET ?";
            return jdbcTemplate.query(sql, userRowMapper, pattern, limit, offset);
        } else {
            String sql = "SELECT id, username, password_hash, created_at, role, status, locked_at, deleted_at FROM users ORDER BY id DESC LIMIT ? OFFSET ?";
            return jdbcTemplate.query(sql, userRowMapper, limit, offset);
        }
    }

    public int countAccounts(String query) {
        if (query != null && !query.trim().isEmpty()) {
            String pattern = "%" + query.trim() + "%";
            String sql = "SELECT COUNT(*) FROM users WHERE username LIKE ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, pattern);
            return count != null ? count : 0;
        } else {
            String sql = "SELECT COUNT(*) FROM users";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        }
    }

    public int count() {
        return countAccounts(null);
    }
}
