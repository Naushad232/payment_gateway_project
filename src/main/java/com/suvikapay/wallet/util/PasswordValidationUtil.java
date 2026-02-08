// src/main/java/com/suvikapay/wallet/util/PasswordValidationUtil.java
package com.suvikapay.wallet.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PasswordValidationUtil {

    private static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public Map<String, String> validatePassword(String password) {
        Map<String, String> errors = new HashMap<>();

        if (password == null || password.isEmpty()) {
            errors.put("password", "Password cannot be null or empty");
            return errors;
        }

        if (password.length() < 8) {
            errors.put("password", "Password must be at least 8 characters long");
        }

        if (password.length() > 128) {
            errors.put("password", "Password cannot exceed 128 characters");
        }

        if (!pattern.matcher(password).matches()) {
            errors.put("password",
                    "Password must contain at least one uppercase letter, one lowercase letter, " +
                            "one digit, and one special character (@#$%^&+=!)");
        }

        // Check for common passwords
        if (isCommonPassword(password)) {
            errors.put("password", "This password is too common. Please choose a stronger password.");
        }

        // Check for username in password
        if (containsUsernamePattern(password)) {
            errors.put("password", "Password should not contain username or email parts");
        }

        // Calculate password strength
        int strength = calculatePasswordStrength(password);
        if (strength < 3) {
            errors.put("password", "Password strength is weak. Consider using a stronger password.");
        }

        return errors;
    }

    public int calculatePasswordStrength(String password) {
        int strength = 0;

        if (password == null || password.isEmpty()) {
            return 0;
        }

        // Length check
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;

        // Character variety check
        if (containsUppercase(password)) strength++;
        if (containsLowercase(password)) strength++;
        if (containsDigit(password)) strength++;
        if (containsSpecialChar(password)) strength++;

        // Entropy check
        if (calculateEntropy(password) > 50) strength++;

        return strength;
    }

    private boolean containsUppercase(String password) {
        return password.matches(".*[A-Z].*");
    }

    private boolean containsLowercase(String password) {
        return password.matches(".*[a-z].*");
    }

    private boolean containsDigit(String password) {
        return password.matches(".*[0-9].*");
    }

    private boolean containsSpecialChar(String password) {
        return password.matches(".*[@#$%^&+=!].*");
    }

    private double calculateEntropy(String password) {
        // Simple entropy calculation
        int poolSize = 0;

        if (containsUppercase(password)) poolSize += 26;
        if (containsLowercase(password)) poolSize += 26;
        if (containsDigit(password)) poolSize += 10;
        if (containsSpecialChar(password)) poolSize += 8; // 8 special characters

        if (poolSize == 0) return 0;

        return password.length() * (Math.log(poolSize) / Math.log(2));
    }

    private boolean isCommonPassword(String password) {
        String[] commonPasswords = {
                "password", "123456", "12345678", "1234", "qwerty", "12345",
                "dragon", "baseball", "football", "letmein", "monkey",
                "abc123", "111111", "master", "hello", "freedom", "whatever",
                "qazwsx", "trustno1", "654321", "jordan23", "harley", "password1",
                "123123", "admin", "welcome", "sunshine", "superman", "iloveyou"
        };

        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.equals(common)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsUsernamePattern(String password) {
        // This would typically check against the user's username or email
        // For now, return false - implement this based on your requirements
        return false;
    }

    public String generatePasswordStrengthMessage(String password) {
        int strength = calculatePasswordStrength(password);

        switch (strength) {
            case 0:
            case 1:
            case 2:
                return "Weak password";
            case 3:
            case 4:
                return "Medium password";
            case 5:
            case 6:
                return "Strong password";
            case 7:
            case 8:
                return "Very strong password";
            default:
                return "Password strength unknown";
        }
    }
}