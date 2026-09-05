package com.chat.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ValidationService {

    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 50;
    public static final int MAX_MESSAGE_LENGTH = 500;

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final List<String> SYSTEM_KEYWORDS = Arrays.asList("ADMIN", "SERVER", "SYSTEM", "ROOT");
    private static final List<Character> DANGEROUS_CHARS = Arrays.asList('\u0000', '\u0001', '\u0002');

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
    }

    public ValidationResult validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return ValidationResult.fail("Tên tài khoản không được để trống");
        }
        username = username.trim();
        if (username.length() < MIN_USERNAME_LENGTH) {
            return ValidationResult.fail("Tên tài khoản phải có ít nhất " + MIN_USERNAME_LENGTH + " ký tự");
        }
        if (username.length() > MAX_USERNAME_LENGTH) {
            return ValidationResult.fail("Tên tài khoản không được vượt quá " + MAX_USERNAME_LENGTH + " ký tự");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return ValidationResult.fail("Tên tài khoản chỉ được chứa chữ, số và dấu gạch dưới");
        }
        if (SYSTEM_KEYWORDS.contains(username.toUpperCase())) {
            return ValidationResult.fail("Tên tài khoản không được sử dụng từ khóa hệ thống");
        }
        return ValidationResult.ok();
    }

    public ValidationResult validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return ValidationResult.fail("Mật khẩu không được để trống");
        }
        password = password.trim();
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return ValidationResult.fail("Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return ValidationResult.fail("Mật khẩu không được vượt quá " + MAX_PASSWORD_LENGTH + " ký tự");
        }
        return ValidationResult.ok();
    }

    public ValidationResult validateMessage(String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            return ValidationResult.fail("Tin nhắn không được để trống");
        }
        if (msg.length() > MAX_MESSAGE_LENGTH) {
            return ValidationResult.fail("Tin nhắn không được vượt quá " + MAX_MESSAGE_LENGTH + " ký tự");
        }
        for (char c : DANGEROUS_CHARS) {
            if (msg.indexOf(c) != -1) {
                return ValidationResult.fail("Tin nhắn chứa ký tự không hợp lệ");
            }
        }
        return ValidationResult.ok();
    }
}
