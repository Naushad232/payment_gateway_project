// src/main/java/com/suvikapay/wallet/util/IPUtils.java
package com.suvikapay.wallet.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class IPUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    private IPUtils() {
        // Private constructor to prevent instantiation
    }

    public static String getClientIP(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

        // Check headers first
        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return getFirstIp(ip);
            }
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    private static String getFirstIp(String ip) {
        if (ip != null && ip.contains(",")) {
            return ip.split(",")[0].trim();
        }
        return ip;
    }

    public static boolean isValidIP(String ip) {
        try {
            if (ip == null || ip.isEmpty()) {
                return false;
            }

            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false;
            }

            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }

            return !ip.endsWith(".");
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static InetAddress parseInetAddress(String ip) {
        try {
            if (isValidIP(ip)) {
                return InetAddress.getByName(ip);
            }
        } catch (UnknownHostException e) {
            log.warn("Failed to parse IP address: {}", ip, e);
        }
        return null;
    }
}