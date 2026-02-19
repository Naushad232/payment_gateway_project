// src/main/java/com/suvikapay/wallet/util/EncryptionUtil.java
package com.suvikapay.wallet.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@Component
public class EncryptionUtil {

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";

    public String encryptText(String plainText, String key) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            byte[] ivBytes = new byte[16];
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            log.error("Error encrypting text", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String encryptFinoText(String plainText, String key, String iv) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            log.error("Error encrypting Fino text", e);
            throw new RuntimeException("Fino encryption failed", e);
        }
    }

    public String decryptText(String encryptedText, String key) {
        try {
            // Extract IV (first 16 chars) and data
            String iv1 = encryptedText.substring(0, 16);
            String data = encryptedText.substring(16);

            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv1.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(data));
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Error decrypting text", e);
            return encryptedText;
        }
    }

    public String decryptFinoText(String encryptedText, String key, String iv) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Error decrypting Fino text", e);
            return encryptedText;
        }
    }

    public String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception e) {
            log.error("Error generating MD5", e);
            throw new RuntimeException("MD5 generation failed", e);
        }
    }

    public String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception e) {
            log.error("Error generating SHA-256", e);
            throw new RuntimeException("SHA-256 generation failed", e);
        }
    }

    public String generateHmacSha256(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
        } catch (Exception e) {
            log.error("Error generating HMAC-SHA256", e);
            throw new RuntimeException("HMAC generation failed", e);
        }
    }

    public BigDecimal decryptBigDecimal(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // Implement your decryption logic for wallet balances
        // This is a placeholder
        return new BigDecimal(encryptedValue);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}