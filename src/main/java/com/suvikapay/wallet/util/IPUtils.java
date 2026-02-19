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

        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return getFirstIp(ip);
            }
        }

        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && !remoteAddr.isEmpty() && !"unknown".equalsIgnoreCase(remoteAddr)) {
            return remoteAddr;
        }

        return "0.0.0.0";
    }

    private static String getFirstIp(String ip) {
        if (ip != null && ip.contains(",")) {
            return ip.split(",")[0].trim();
        }
        return ip;
    }

    public static boolean isValidIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // Remove IPv6 prefix if present
        if (ip.startsWith("::ffff:")) {
            ip = ip.substring(7);
        }

        String ipv4Regex = "^(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}$";
        return ip.matches(ipv4Regex);
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