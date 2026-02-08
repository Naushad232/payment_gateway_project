// src/main/java/com/suvikapay/wallet/validation/PasswordValidator.java
package com.suvikapay.wallet.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // Password pattern:
    // - At least 8 characters
    // - At least one uppercase letter
    // - At least one lowercase letter  
    // - At least one digit
    // - At least one special character (@#$%^&+=!)
    // - No whitespace allowed
    private static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    // Alternative: Less strict pattern for flexibility
    private static final String PASSWORD_PATTERN_LENIENT =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";

    private Pattern pattern;
    private boolean strictValidation;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // You can configure strict/lenient validation via properties if needed
        this.strictValidation = true; // Default to strict
        this.pattern = Pattern.compile(strictValidation ? PASSWORD_PATTERN : PASSWORD_PATTERN_LENIENT);
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            log.debug("Password is null or empty");
            addConstraintViolation(context, "Password cannot be null or empty");
            return false;
        }

        if (password.length() < 8) {
            log.debug("Password too short: {}", password.length());
            addConstraintViolation(context, "Password must be at least 8 characters long");
            return false;
        }

        if (password.length() > 128) {
            log.debug("Password too long: {}", password.length());
            addConstraintViolation(context, "Password cannot exceed 128 characters");
            return false;
        }

        boolean matchesPattern = pattern.matcher(password).matches();

        if (!matchesPattern) {
            log.debug("Password does not meet complexity requirements");

            if (strictValidation) {
                addConstraintViolation(context,
                        "Password must contain at least one uppercase letter, one lowercase letter, " +
                                "one digit, and one special character (@#$%^&+=!)");
            } else {
                addConstraintViolation(context,
                        "Password must contain at least one uppercase letter, one lowercase letter, " +
                                "and one digit");
            }
            return false;
        }

        // Additional checks
        if (containsCommonPattern(password)) {
            log.debug("Password contains common patterns");
            addConstraintViolation(context, "Password contains common patterns that are easy to guess");
            return false;
        }

        if (containsSequentialCharacters(password)) {
            log.debug("Password contains sequential characters");
            addConstraintViolation(context, "Password contains sequential characters");
            return false;
        }

        if (containsRepeatedCharacters(password)) {
            log.debug("Password contains repeated characters");
            addConstraintViolation(context, "Password contains too many repeated characters");
            return false;
        }

        return true;
    }

    private void addConstraintViolation(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation();
    }

    private boolean containsCommonPattern(String password) {
        String lowerPassword = password.toLowerCase();

        // Check for common patterns
        String[] commonPatterns = {
                "password", "123456", "qwerty", "abc123", "admin", "welcome",
                "letmein", "monkey", "dragon", "baseball", "football", "master"
        };

        for (String pattern : commonPatterns) {
            if (lowerPassword.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsSequentialCharacters(String password) {
        // Check for sequential characters (e.g., abc, 123, etc.)
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);

            // Check sequential letters
            if (Character.isLetter(c1) && Character.isLetter(c2) && Character.isLetter(c3)) {
                if (c1 + 1 == c2 && c2 + 1 == c3) {
                    return true;
                }
            }

            // Check sequential numbers
            if (Character.isDigit(c1) && Character.isDigit(c2) && Character.isDigit(c3)) {
                if (c1 + 1 == c2 && c2 + 1 == c3) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean containsRepeatedCharacters(String password) {
        // Check for repeated characters (e.g., aaa, 111, etc.)
        for (int i = 0; i < password.length() - 2; i++) {
            if (password.charAt(i) == password.charAt(i + 1) &&
                    password.charAt(i) == password.charAt(i + 2)) {
                return true;
            }
        }

        return false;
    }
}