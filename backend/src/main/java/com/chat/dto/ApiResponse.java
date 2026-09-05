package com.chat.dto;

public class ApiResponse {
    private boolean ok;
    private String message;

    public ApiResponse() {}

    public ApiResponse(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public static ApiResponse ok(String message) {
        return new ApiResponse(true, message);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(false, message);
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
