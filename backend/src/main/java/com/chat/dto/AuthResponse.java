package com.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private boolean ok;
    private String token;
    private String username;
    private String message;

    public AuthResponse() {}

    public AuthResponse(boolean ok, String token, String username, String message) {
        this.ok = ok;
        this.token = token;
        this.username = username;
        this.message = message;
    }

    public static AuthResponse success(String token, String username) {
        return new AuthResponse(true, token, username, null);
    }

    public static AuthResponse success(String token, String username, String message) {
        return new AuthResponse(true, token, username, message);
    }

    public static AuthResponse error(String message) {
        return new AuthResponse(false, null, null, message);
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
