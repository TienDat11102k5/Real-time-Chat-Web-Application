package com.chat.model;

public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String createdAt;
    private String role = "user";       // "user", "moderator", "admin"
    private String status = "active";   // "active", "locked"
    private String lockedAt;
    private String deletedAt;

    public User() {}

    public User(Long id, String username, String passwordHash, String createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public User(Long id, String username, String passwordHash, String createdAt, String role, String status, String lockedAt, String deletedAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.role = (role != null) ? role : "user";
        this.status = (status != null) ? status : "active";
        this.lockedAt = lockedAt;
        this.deletedAt = deletedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(String lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(String deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isLocked() {
        return "locked".equalsIgnoreCase(status);
    }

    public boolean isDeleted() {
        return deletedAt != null && !deletedAt.trim().isEmpty();
    }
}
