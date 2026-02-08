// src/main/java/com/suvikapay/wallet/util/AppConstants.java
package com.suvikapay.wallet.util;

public class AppConstants {

    // User roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_AGENT = "AGENT";
    public static final String ROLE_USER = "USER";

    // Transaction types
    public static final String TXN_TYPE_PAYIN = "PAYIN";
    public static final String TXN_TYPE_PAYOUT = "PAYOUT";

    // Transaction status
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PROCESSING = "PROCESSING";

    // API Status
    public static final String API_STATUS_ACTIVE = "ACTIVE";
    public static final String API_STATUS_INACTIVE = "INACTIVE";
    public static final String API_STATUS_SUSPENDED = "SUSPENDED";

    // Charge types
    public static final String CHARGE_TYPE_FLAT = "FLAT";
    public static final String CHARGE_TYPE_PERCENTAGE = "PERCENTAGE";

    // Date formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String ISO_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Validation
    public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    public static final String PHONE_REGEX = "^[0-9]{10}$";
    public static final String PAN_REGEX = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$";

    // Error messages
    public static final String ERROR_INVALID_CREDENTIALS = "Invalid email or password";
    public static final String ERROR_USER_DISABLED = "User account is disabled";
    public static final String ERROR_USER_LOCKED = "User account is locked";
    public static final String ERROR_UNAUTHORIZED = "Unauthorized access";
    public static final String ERROR_FORBIDDEN = "Access denied";
    public static final String ERROR_NOT_FOUND = "Resource not found";
    public static final String ERROR_VALIDATION_FAILED = "Validation failed";
    public static final String ERROR_INTERNAL_SERVER = "Internal server error";

    private AppConstants() {
        // Private constructor to prevent instantiation
    }
}