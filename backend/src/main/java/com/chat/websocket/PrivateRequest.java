package com.chat.websocket;

public class PrivateRequest {
    private final String sender;
    private final String receiver;
    private final String preview;
    private final long createdAt;

    public PrivateRequest(String sender, String receiver, String preview) {
        this.sender = sender;
        this.receiver = receiver;
        this.preview = preview;
        this.createdAt = System.currentTimeMillis();
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getPreview() {
        return preview;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(long timeoutSeconds) {
        return (System.currentTimeMillis() - createdAt) > (timeoutSeconds * 1000);
    }

    public long getRemainingSeconds(long timeoutSeconds) {
        long elapsed = (System.currentTimeMillis() - createdAt) / 1000;
        return Math.max(0, timeoutSeconds - elapsed);
    }
}
