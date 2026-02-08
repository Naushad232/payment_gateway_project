// src/main/java/com/suvikapay/wallet/util/CommonUtils.java
package com.suvikapay.wallet.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
public class CommonUtils {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    public static String generateSecureRandomToken(int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    public static String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static String generateOrderId() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public static String generateUserId() {
        return "USR" + System.currentTimeMillis();
    }

    public static OffsetDateTime getCurrentUTCDateTime() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    public static String formatDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    public static String maskString(String str, int start, int end, char maskChar) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        if (start < 0) start = 0;
        if (end > str.length()) end = str.length();
        if (start > end) return str;

        int maskLength = end - start;
        if (maskLength == 0) return str;

        StringBuilder maskedString = new StringBuilder(str);
        for (int i = start; i < end; i++) {
            maskedString.setCharAt(i, maskChar);
        }
        return maskedString.toString();
    }

    public static String maskEmail(String email) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return email;
        }

        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return email;
        }

        String maskedUsername = username.charAt(0) + "***" + username.charAt(username.length() - 1);
        return maskedUsername + "@" + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 10) {
            return phone;
        }

        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }
}