package com.chat.model;

public class AuditLog {
    private Long id;
    private String timestamp;
    private String adminUsername;
    private String action;
    private String target;
    private String result;
    private String details;

    public AuditLog() {
    }

    public AuditLog(Long id, String timestamp, String adminUsername, String action, String target, String result, String details) {
        this.id = id;
        this.timestamp = timestamp;
        this.adminUsername = adminUsername;
        this.action = action;
        this.target = target;
        this.result = result;
        this.details = details;
    }

    public AuditLog(String timestamp, String adminUsername, String action, String target, String result, String details) {
        this(null, timestamp, adminUsername, action, target, result, details);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
