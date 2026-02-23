// src/main/java/com/suvikapay/wallet/service/payin/impl/PayinServiceImpl.java
package com.suvikapay.wallet.service.payin.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvikapay.wallet.dto.payin.*;
import com.suvikapay.wallet.dto.response.UserResponse;
import com.suvikapay.wallet.entity.*;
import com.suvikapay.wallet.exception.ResourceNotFoundException;
import com.suvikapay.wallet.exception.ServiceException;
import com.suvikapay.wallet.exception.UnauthorizedException;
import com.suvikapay.wallet.repository.*;
import com.suvikapay.wallet.service.payin.PayinService;
import com.suvikapay.wallet.service.UserService;
import com.suvikapay.wallet.util.AppConstants;
import com.suvikapay.wallet.util.CommonUtils;
import com.suvikapay.wallet.util.EncryptionUtil;
import com.suvikapay.wallet.util.IPUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.UserTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayinServiceImpl implements PayinService {

    private final PayinTransactionRepository payinTransactionRepository;
    private final ApiLogRepository apiLogRepository;
    private final UserTransactionRepository userTransactionRepository;
    private final UserIpRepository userIpRepository;
    private final UserChargeSlabRepository userChargeSlabRepository;
    private final MerchantRepository merchantRepository;
    private final SystemSettingsRepository systemSettingsRepository;
    private final UserService userService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final EncryptionUtil encryptionUtil;
    private final HttpServletRequest request;

//    @Value("${app.jwt.secret}")
//    private String jwtSecret;

    // Constants for various merchant APIs
    private static final String ROYALE_API_URL = "https://api.myRoyalPay.in/api/generateQrAuth";
    private static final String FINO_API_URL = "https://api.sprintnxt.in/api/v2/UPIService/UPI";
    private static final String AIRPAY_ENCRYPT_URL = "https://kraken.airpay.co.in/airpay/api/generateOrder";
    private static final String AIRPAY_STATUS_URL = "https://kraken.airpay.co.in/airpay/order/verify.php";
    private static final String VIMO_AUTH_URL = "https://prod.vidual.in/payinapi/api/Signature/authorize";
    private static final String VIMO_PAYIN_URL = "https://prod.vidual.in/payinapi/api/Payment/upi";
    private static final String DIGI_PAYIN_URL = "https://walletupi.paypointz.com/upi/getqrcodestring";
    private static final String DIGI_QR_URL = "https://walletupi.paypointz.com/upi/generatedynamicqr";
    private static final String ENCRYPTION_SERVICE_URL = "https://suvikapay.com/encryption.php";

    @Value("${idfc.auth.url:https://gateway.suvikapay.com/auth/token}")
    private String idfcAuthUrl;

    @Value("${idfc.generate.upi.url:https://gateway.suvikapay.com/api/v6/generateUpi}")
    private String idfcGenerateUpiUrl;

    @Value("${idfc.auth.username:}")
    private String idfcAuthUsername;

    @Value("${idfc.auth.password:}")
    private String idfcAuthPassword;

    private String cachedIdfcToken;
    private OffsetDateTime cachedIdfcTokenExpiry;
    private final Object idfcTokenLock = new Object();

    @Override
    @Transactional
    public PayinResponseDto generatePaymentLink(CreatePaymentLinkDto request, Integer userId, String appName) {
        try {
            log.info("Generating payment link for user: {}, request: {}", userId, request);

            // Get logged in user as AppUser (not UserResponse)
            AppUser user = userService.getAppUserById(userId);

            // Check system settings
            SystemSettings settings = getSystemSettings();

            if (settings.getMaintainence()) {
                return PayinResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("System is under maintenance. Please wait for update from admin.")
                        .responseCode(200)
                        .build();
            }

            if (!settings.getPayinService()) {
                return PayinResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Payin services are not available at the moment. Please wait for update from admin.")
                        .responseCode(200)
                        .build();
            }

            // Validate amount
            BigDecimal amount = request.getOrderAmount();
            if (amount.compareTo(BigDecimal.TEN) < 0 || amount.compareTo(new BigDecimal("100000")) > 0) {
                return PayinResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Amount should be between 10 and 100000")
                        .responseCode(200)
                        .build();
            }

            // Check merchant-specific amount limits
            Merchant payingMerchant = user.getPayingMerchant();
            String merchantName = payingMerchant != null ? payingMerchant.getMerchantName() : "";

            if ("DIGI".equals(merchantName) &&
                    (amount.compareTo(new BigDecimal("500")) < 0 || amount.compareTo(new BigDecimal("2500")) > 0)) {
                return PayinResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Amount should be between 500 and 2500 as per guidelines")
                        .responseCode(200)
                        .build();
            }

            // Validate order ID length
            if (request.getOrderId().length() < 11) {
                return PayinResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Reference number should be at least 11 characters")
                        .responseCode(200)
                        .build();
            }

            // Check for duplicate order ID
            Optional<PayinTransaction> existingTxn = payinTransactionRepository.findByOrderId(request.getOrderId());
            if (existingTxn.isPresent()) {
                return PayinResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Duplicate Reference No. Please try with a different reference.")
                        .responseCode(200)
                        .build();
            }

            // Check user API status
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                return PayinResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("We are taking downtime due to technical issues. Please wait for further update.")
                        .responseCode(200)
                        .build();
            }

            // IP Whitelist validation - FIXED: Using HttpServletRequest from controller
            String clientIp = IPUtils.getClientIP(this.request); // Use the injected HttpServletRequest
            List<UserIp> userIps = userIpRepository.findByUser(user);

            if (!userIps.isEmpty()) {
                boolean ipAllowed = userIps.stream()
                        .anyMatch(ip -> ip.getIpAddress().getHostAddress().equals(clientIp));

                if (!ipAllowed) {
                    return PayinResponseDto.builder()
                            .status(false)
                            .error(true)
                            .message("Your IP is not whitelisted. Requested IP: " + clientIp)
                            .responseCode(200)
                            .build();
                }
                log.info("IP whitelisted: {}, processing further...", clientIp);
            } else {
                log.info("No IP whitelist configured for user {}, skipping IP validation", user.getUserId());
            }

            // Generate transaction ID
            String txnId = generateTransactionId();

            // Route to appropriate merchant handler
            PayinResponseDto result;

            if ("NESTPAY".equals(appName)) {
                switch (merchantName) {
                    case "AIRPAY":
                        result = paynitAirPayGeneratePaymentLink(user, txnId, request);
                        break;
                    case "ROYALE":
                        result = royalePayGeneratePaymentLink(user, txnId, request);
                        break;
                    case "FINO":
                        result = finoPayGeneratePaymentLink(user, txnId, request);
                        break;
                    case "VIMO":
                        result = vimoPayin(user, txnId, request);
                        break;
                    case "IDFC":
                        result = idfcPayin(user, txnId, request);
                        break;
                    case "DIGI":
                        result = digiKhataPayin(user, txnId, request);
                        break;
                    case "RABI":
                        result = rabiPayin(user, txnId, request);
                        break;
                    default:
                        result = PayinResponseDto.builder()
                                .status(false)
                                .error(true)
                                .message("No merchant assigned for payin")
                                .responseCode(400)
                                .build();
                }
            } else {
                result = PayinResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Invalid app name")
                        .responseCode(400)
                        .build();
            }

            return result;

        } catch (Exception e) {
            log.error("Error generating payment link", e);
            throw new ServiceException("Failed to generate payment link: " + e.getMessage());
        }
    }



    // Merchant-specific implementations

    private PayinResponseDto royalePayGeneratePaymentLink(AppUser user, String txnId, CreatePaymentLinkDto request) {
        try {
            String url = String.format("%s?memberid=MRP18638893&txnpwd=784921&name=%s&amount=%s&txnid=%s",
                    ROYALE_API_URL, request.getName(), request.getOrderAmount(), request.getOrderId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> result = response.getBody();

            // Save API log
            saveApiLog(user.getUserId(),txnId, url, objectMapper.writeValueAsString(result),
                    "GENERATEUPI", "ROYALE", null);

            if (result != null && "200".equals(result.get("status_code")) && "Success".equals(result.get("status"))) {
                // Save payin transaction
                savePayinTransaction(user, txnId, request, "ROYALE", "PENDING");

                return PayinResponseDto.builder()
                        .status(true)
                        .error(false)
                        .message("SUCCESS")
                        .responseCode(200)
                        .data(PayinDataDto.builder()
                                .paymentLink((String) result.get("qr"))
                                .paymentProcessUrl((String) result.get("qr"))
                                .referenceId((String) result.get("txnID"))
                                .transactionId((String) result.get("txnID"))
                                .status("SUCCESS")
                                .build())
                        .build();
            } else {
                return PayinResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("FAILED")
                        .responseCode(result != null ? (Integer) result.get("status_code") : 500)
                        .data(PayinDataDto.builder()
                                .referenceId(request.getOrderId())
                                .transactionId(txnId)
                                .status("FAILED")
                                .build())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error in Royale payin", e);
            return PayinResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message(e.getMessage())
                    .responseCode(503)
                    .build();
        }
    }

    private PayinResponseDto finoPayGeneratePaymentLink(AppUser user, String txnId, CreatePaymentLinkDto request) {
        try {
            // Prepare request data
            Map<String, Object> postData = new HashMap<>();
            postData.put("apiId", "20260");
            postData.put("bankId", "3");
            postData.put("amount", request.getOrderAmount().toString());
            postData.put("payeeVPA", "ps1.wager@finobank");
            postData.put("mobile", request.getMobile());
            postData.put("ExpiryTime", "10");
            postData.put("txnNote", "TESTQR");
            postData.put("txnReferance", request.getOrderId());

            // Prepare client token
            Map<String, Object> clientToken = new HashMap<>();
            clientToken.put("client_secret", "677da8a1b98217760bde9925c5a512a836ad5d8f268d9c55be5e0238b81d1568");
            clientToken.put("requestid", generateRandomNumber(123121, 990999));
            clientToken.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));

            String encrypted = encryptionUtil.encryptFinoText(
                    objectMapper.writeValueAsString(clientToken),
                    "4faa506feadea9bd7bc7125df0bdee26",
                    "c13453b649bc8ef3");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", encrypted);
            headers.set("Client-id", "U1BSX05YVF9wcm9kX2ViN2JkZDUyNjg3ZTE3Yzc=");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(postData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(FINO_API_URL, entity, Map.class);
            Map<String, Object> result = response.getBody();

            saveApiLog(user.getUserId(),txnId, objectMapper.writeValueAsString(postData),
                    objectMapper.writeValueAsString(result), "GENERATEUPI", "FINO", null);

            if (result != null && "200".equals(result.get("status_code")) && Boolean.TRUE.equals(result.get("status"))) {
                Map<String, Object> details = (Map<String, Object>) result.get("details");

                savePayinTransaction(user, txnId, request, "FINO", "PENDING");

                return PayinResponseDto.builder()
                        .status(true)
                        .error(false)
                        .message("SUCCESS")
                        .responseCode(200)
                        .data(PayinDataDto.builder()
                                .paymentLink((String) details.get("intent_url"))
                                .paymentProcessUrl((String) details.get("intent_url"))
                                .referenceId((String) details.get("txnReferance"))
                                .transactionId((String) details.get("UPIRefID"))
                                .status("SUCCESS")
                                .build())
                        .build();
            } else {
                return PayinResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("FAILED")
                        .responseCode(result != null ? (Integer) result.get("status_code") : 500)
                        .data(PayinDataDto.builder()
                                .referenceId(request.getOrderId())
                                .transactionId(txnId)
                                .status("FAILED")
                                .build())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error in Fino payin", e);
            return PayinResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message(e.getMessage())
                    .responseCode(503)
                    .build();
        }
    }

    private PayinResponseDto vimoPayin(AppUser user, String txnId, CreatePaymentLinkDto request) {
        try {
            // Get Vimo access token
            String bearerToken = getVimoAccessToken();

            // Generate random lat long
            Map<String, Double> latLong = getRandomLatLong();

            Map<String, Object> dataToEncrypt = new HashMap<>();
            dataToEncrypt.put("userMobileNo", generateIndianMobileNumber());
            dataToEncrypt.put("merchantRefId", request.getOrderId());
            dataToEncrypt.put("amount", request.getOrderAmount());
            dataToEncrypt.put("lat", latLong.get("latitude"));
            dataToEncrypt.put("long", latLong.get("longitude"));
            dataToEncrypt.put("udf1", "");
            dataToEncrypt.put("udf2", "");
            dataToEncrypt.put("udf3", "");

            // Encrypt data using external service
            String encryptedPayload = getEncryptedData(dataToEncrypt);

            Map<String, Object> requestBody = Map.of("requestBody", encryptedPayload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(bearerToken);
            headers.set("userId", "3678e7ba-d3e3-47e0-9676-ab379389bc6a");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(VIMO_PAYIN_URL, entity, Map.class);
            Map<String, Object> encryptedResponse = response.getBody();

            // Decrypt response
            String decryptedResponse = getDecryptedData((String) encryptedResponse.get("data"));
            Map<String, Object> finalResult = objectMapper.readValue(decryptedResponse, Map.class);

            saveApiLog(user.getUserId(),txnId, objectMapper.writeValueAsString(dataToEncrypt),
                    objectMapper.writeValueAsString(finalResult), "GENERATEUPI", "VIMO", null);

            if (Boolean.TRUE.equals(encryptedResponse.get("successStatus"))) {
                savePayinTransaction(user, txnId, request, "VIMO", "PENDING");

                return PayinResponseDto.builder()
                        .status(true)
                        .error(false)
                        .message("SUCCESS")
                        .responseCode(200)
                        .data(PayinDataDto.builder()
                                .paymentLink((String) finalResult.get("upiIntend"))
                                .paymentProcessUrl((String) finalResult.get("qr"))
                                .referenceId(request.getOrderId())
                                .transactionId(txnId)
                                .status("SUCCESS")
                                .build())
                        .build();
            } else {
                return PayinResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("FAILED")
                        .responseCode(200)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error in Vimo payin", e);
            return PayinResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message(e.getMessage())
                    .responseCode(503)
                    .build();
        }
    }

//    private PayinResponseDto idfcPayin(AppUser user, String txnId, CreatePaymentLinkDto request) {
//        try {
//            String tId = generateRandomStringIDFC();
//
//            // Create UPI string
//            String upiLink = String.format(
//                    "upi://pay?ver=01&mode=01&pa=suvika@idfcbank&pn=Suvika&mtid=TIDSUVIKA&mid=MIDSUVIKA&cu=INR&mc=7210&tr=%s&tn=EMIPayment&am=%s",
//                    request.getOrderId(), request.getOrderAmount());
//
//            saveApiLog(user.getUserId(),txnId, upiLink, upiLink, "GENERATEUPI", "IDFC-PAYIN", null);
//
//            // Save transaction
//            PayinTransaction transaction = PayinTransaction.builder()
//                    .userId(user.getUserId())
//                    .userName(user.getName())
//                    .txnId(txnId)
//                    .tId(tId)
//                    .orderId(request.getOrderId())
//                    .amount(request.getOrderAmount())
//                    .status("PENDING")
//                    .api("IDFC")
//                    .ipAddress(IPUtils.parseInetAddress(IPUtils.getClientIP(this.request)))
//                    .createdAt(OffsetDateTime.now())
//                    .updatedAt(OffsetDateTime.now())
//                    .build();
//
//            payinTransactionRepository.save(transaction);
//
//            return PayinResponseDto.builder()
//                    .status(true)
//                    .error(false)
//                    .message("SUCCESS")
//                    .responseCode(200)
//                    .data(PayinDataDto.builder()
//                            .paymentLink(upiLink)
//                            .paymentProcessUrl("")
//                            .referenceId(request.getOrderId())
//                            .transactionId(txnId)
//                            .status("SUCCESS")
//                            .build())
//                    .build();
//
//        } catch (Exception e) {
//            log.error("Error in IDFC payin", e);
//            return PayinResponseDto.builder()
//                    .status(false)
//                    .error(true)
//                    .message(e.getMessage())
//                    .responseCode(503)
//                    .build();
//        }
//    }
    private PayinResponseDto idfcPayin(AppUser user, String txnId, CreatePaymentLinkDto request) {
        try {
            Merchant payingMerchant = user.getPayingMerchant();
            if (payingMerchant == null) {
                return PayinResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("No paying merchant mapped for userId=" + user.getUserId())
                        .responseCode(400)
                        .build();
            }

            // 1) Ensure we have a valid token (cached for 20 minutes)
            String bearerToken = getIdfcBearerToken();

            // 2) Call IDFC generate UPI API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("order_id", request.getOrderId());
            requestBody.put("order_amount", request.getOrderAmount());
            requestBody.put("name", request.getName());
            requestBody.put("mobile", request.getMobile());
            requestBody.put("email", request.getEmail());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(bearerToken);

            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(idfcGenerateUpiUrl, httpEntity, Map.class);
            Map<String, Object> result = response.getBody();

            saveApiLog(user.getUserId(), txnId,
                    objectMapper.writeValueAsString(requestBody),
                    objectMapper.writeValueAsString(result),
                    "GENERATEUPI", "IDFC-PAYIN", null);

            if (response.getStatusCode().is2xxSuccessful() && result != null && Boolean.TRUE.equals(result.get("status"))) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");

                // Persist transaction with merchant info (to avoid null merchant_id)
                PayinTransaction transaction = PayinTransaction.builder()
                        .userId(user.getUserId())
                        .userName(user.getName())
                        .merchant(payingMerchant)
                        .txnId(txnId)
                        .tId((String) data.getOrDefault("transactionId", generateRandomStringIDFC()))
                        .orderId(request.getOrderId())
                        .amount(request.getOrderAmount())
                        .status("PENDING")
                        .api("IDFC")
                        .ipAddress(IPUtils.parseInetAddress(IPUtils.getClientIP(this.request)))
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build();

                payinTransactionRepository.save(transaction);

                return PayinResponseDto.builder()
                        .status(true)
                        .error(false)
                        .message(String.valueOf(result.getOrDefault("message", "SUCCESS")))
                        .responseCode(((Number) result.getOrDefault("responseCode", 200)).intValue())
                        .data(PayinDataDto.builder()
                                .paymentLink((String) data.get("payment_link"))
                                .paymentProcessUrl((String) data.get("PaymentProcessUrl"))
                                .referenceId((String) data.getOrDefault("ReferenceId", request.getOrderId()))
                                .transactionId((String) data.getOrDefault("transactionId", txnId))
                                .status((String) data.getOrDefault("status", "SUCCESS"))
                                .build())
                        .build();
            }

            // Non-success path
            String message = result != null ? String.valueOf(result.getOrDefault("message", "FAILED")) : "FAILED";
            int responseCode = result != null ? ((Number) result.getOrDefault("responseCode", 500)).intValue() : 500;

            return PayinResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message(message)
                    .responseCode(responseCode)
                    .data(PayinDataDto.builder()
                            .referenceId(request.getOrderId())
                            .transactionId(txnId)
                            .status("FAILED")
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Error in IDFC payin", e);
            return PayinResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message(e.getMessage())
                    .responseCode(503)
                    .build();
        }
    }

    /**
     * Obtain bearer token for IDFC gateway. Token is cached for 20 minutes to avoid
     * hitting auth endpoint on every request.
     */
    private String getIdfcBearerToken() throws JsonProcessingException {
        synchronized (idfcTokenLock) {
            if (cachedIdfcToken != null && cachedIdfcTokenExpiry != null &&
                    OffsetDateTime.now().isBefore(cachedIdfcTokenExpiry.minus(Duration.ofMinutes(1)))) {
                return cachedIdfcToken;
            }

            if (idfcAuthUsername == null || idfcAuthUsername.isBlank() ||
                    idfcAuthPassword == null || idfcAuthPassword.isBlank()) {
                throw new ServiceException("IDFC credentials are not configured");
            }

            Map<String, String> authBody = new HashMap<>();
            authBody.put("user_name", idfcAuthUsername);
            authBody.put("password", idfcAuthPassword);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(authBody, headers);
            ResponseEntity<Map> authResponse = restTemplate.postForEntity(idfcAuthUrl, httpEntity, Map.class);
            Map<String, Object> result = authResponse.getBody();

            if (authResponse.getStatusCode().is2xxSuccessful() && result != null && result.get("token") != null) {
                cachedIdfcToken = (String) result.get("token");
                cachedIdfcTokenExpiry = OffsetDateTime.now().plusMinutes(20);
                return cachedIdfcToken;
            }

            String message = result != null ? String.valueOf(result.getOrDefault("message", "Auth failed")) : "Auth failed";
            throw new UnauthorizedException(message);
        }
    }

    private PayinResponseDto digiKhataPayin(AppUser user, String txnId, CreatePaymentLinkDto request) {
        try {
            Map<String, Double> latLong = getRandomLatLong();

            Map<String, Object> data = new HashMap<>();
            data.put("OrderId", request.getOrderId());
            data.put("Remarks", "For Limit");
            data.put("CollectExpiryAfter", "30");
            data.put("Amount", request.getOrderAmount());
            data.put("Latitude", latLong.get("latitude"));
            data.put("Longitude", latLong.get("longitude"));
            data.put("Location", "400097");
            data.put("IPAddress", "65.1.212.67");
            data.put("DeviceSerial", "Chrome");
            data.put("DeviceOS", "Windows");
            data.put("AppTechName", "com.digikhata.in");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("MerchantID", "69564");
            headers.set("AuthKey", "X7j#4Qd9!Lm2PzT@8VkYwCb^NhFs0");
            headers.set("InterfaceKey", "M5r$Kt1PxQ9!ZnJ#V7LdWb@CYF4mT");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);

            // Get QR code string
            ResponseEntity<Map> response = restTemplate.postForEntity(DIGI_PAYIN_URL, entity, Map.class);
            Map<String, Object> result = response.getBody();

            // Get QR code image
            ResponseEntity<Map> qrResponse = restTemplate.postForEntity(DIGI_QR_URL, entity, Map.class);
            Map<String, Object> qrResult = qrResponse.getBody();

            Map<String, Object> resultData = (Map<String, Object>) result.get("data");
            Map<String, Object> qrData = (Map<String, Object>) qrResult.get("data");

            saveApiLog(user.getUserId(),txnId, objectMapper.writeValueAsString(data),
                    objectMapper.writeValueAsString(result), "GENERATEUPI", "DIGI-PAYIN", null);

            // Save transaction
            PayinTransaction transaction = PayinTransaction.builder()
                    .userId(user.getUserId())
                    .userName(user.getName())
                    .txnId(txnId)
                    .tId("")
                    .orderId(request.getOrderId())
                    .amount(request.getOrderAmount())
                    .status("PENDING")
                    .api("DIGI")
                    .ipAddress(IPUtils.parseInetAddress(IPUtils.getClientIP(this.request)))
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            payinTransactionRepository.save(transaction);

            return PayinResponseDto.builder()
                    .status(true)
                    .error(false)
                    .message("SUCCESS")
                    .responseCode(200)
                    .data(PayinDataDto.builder()
                            .paymentLink((String) resultData.get("qrCodeString"))
                            .paymentProcessUrl((String) qrData.get("qrCodeImage"))
                            .referenceId(request.getOrderId())
                            .transactionId(txnId)
                            .status("SUCCESS")
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Error in DigiKhata payin", e);
            return PayinResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message(e.getMessage())
                    .responseCode(503)
                    .build();
        }
    }

    private PayinResponseDto rabiPayin(AppUser user, String txnId, CreatePaymentLinkDto request) {
        try {
            Map<String, Object> apiPayload = new HashMap<>();
            apiPayload.put("amount", request.getOrderAmount());
            apiPayload.put("order_id", request.getOrderId());
            apiPayload.put("payment_method", "UPI");
            apiPayload.put("mobile", generateIndianMobileNumber());

            // Generate signature
            String rawBody = objectMapper.writeValueAsString(apiPayload);
            String signature = encryptionUtil.generateHmacSha256(
                    rawBody,
                    "XUQByj3yS8S7yPkrLXfekqvLqhzZH2BcxtK50emD9h83zc0Upb+MHEwUyzyG7EJvK8cKohb+tnS9KeGYCOV2ipkm3ayHUp/h8k5Q4qnAIQY=");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("rabipay-client-id", "a65879cd44856707fdc20c74");
            headers.set("rabipay-signature", signature);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(apiPayload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.kadyanindustries.com/gateway/v1/payin", entity, Map.class);
            Map<String, Object> result = response.getBody();

            saveApiLog(user.getUserId(),txnId, objectMapper.writeValueAsString(apiPayload),
                    objectMapper.writeValueAsString(result), "GENERATEUPI", "RABI-PAYIN", null);

            Map<String, Object> resultData = (Map<String, Object>) result.get("data");

            PayinTransaction transaction = PayinTransaction.builder()
                    .userId(user.getUserId())
                    .userName(user.getName())
                    .txnId(txnId)
                    .tId("")
                    .orderId(request.getOrderId())
                    .amount(request.getOrderAmount())
                    .status("PENDING")
                    .api("RABI")
                    .ipAddress(IPUtils.parseInetAddress(IPUtils.getClientIP(this.request)))
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            payinTransactionRepository.save(transaction);

            return PayinResponseDto.builder()
                    .status(true)
                    .error(false)
                    .message("SUCCESS")
                    .responseCode(200)
                    .data(PayinDataDto.builder()
                            .paymentLink((String) resultData.get("qr_code"))
                            .paymentProcessUrl((String) resultData.get("qr_code"))
                            .referenceId(request.getOrderId())
                            .transactionId(txnId)
                            .status("SUCCESS")
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Error in Rabi payin", e);
            return PayinResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message(e.getMessage())
                    .responseCode(503)
                    .build();
        }
    }

    private PayinResponseDto paynitAirPayGeneratePaymentLink(AppUser user, String txnId, CreatePaymentLinkDto request) {
        try {
            String mercid = "334138";
            String username = "j6bUja92K5";
            String password = "wWeNyN2j";
            String secret = "5wK3fDKNkRBgpnHq";

            String key256 = encryptionUtil.sha256(username + "~:~" + password);

            String mer_dom = Base64.getEncoder().encodeToString("https://suvikapay.com".getBytes());
            String call_type = "upiqr";
            String currentDate = formatDate(new Date());

            String alldata = mercid + request.getOrderId() +
                    request.getOrderAmount().setScale(2, RoundingMode.HALF_UP).toString() +
                    request.getMobile() + request.getEmail() + mer_dom + call_type;

            String checksum = encryptionUtil.sha256(key256 + "@" + alldata);

            Map<String, Object> fields = new HashMap<>();
            fields.put("mercid", mercid);
            fields.put("orderid", request.getOrderId());
            fields.put("amount", request.getOrderAmount().setScale(2, RoundingMode.HALF_UP).toString());
            fields.put("buyerPhone", request.getMobile());
            fields.put("buyerEmail", request.getEmail());
            fields.put("mer_dom", mer_dom);
            fields.put("call_type", call_type);

            String jsonData = objectMapper.writeValueAsString(fields);
            String encKey = encryptionUtil.md5(secret);
            String encData = encryptionUtil.encryptText(jsonData, encKey);

            Map<String, Object> postFields = new HashMap<>();
            postFields.put("encData", encData);
            postFields.put("checksum", checksum);
            postFields.put("mercid", mercid);

            String encryptedRawData = callAirpayEncryptApi(postFields);
            String decryptedData = encryptionUtil.decryptText(encryptedRawData, encKey);

            saveApiLog(user.getUserId(),txnId, jsonData, decryptedData, "GENERATEUPI", "AIRPAY", null);

            Map<String, Object> result = objectMapper.readValue(decryptedData, Map.class);

            if (200 == ((Number) result.get("status")).intValue()) {
                savePayinTransaction(user, txnId, request, "AIRPAY", "PENDING");

                return PayinResponseDto.builder()
                        .status(true)
                        .error(false)
                        .message("SUCCESS")
                        .responseCode(200)
                        .data(PayinDataDto.builder()
                                .paymentLink((String) result.get("QRCODE_STRING"))
                                .paymentProcessUrl((String) result.get("QRCODE_STRING"))
                                .referenceId(request.getOrderId())
                                .transactionId(txnId)
                                .status("SUCCESS")
                                .build())
                        .build();
            }

            return PayinResponseDto.builder()
                    .status(true)
                    .error(false)
                    .message("Ok")
                    .responseCode(200)
                    .build();

        } catch (Exception e) {
            log.error("Error in AirPay payin", e);
            return PayinResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message(e.getMessage())
                    .responseCode(503)
                    .build();
        }
    }

    // Helper methods

    private SystemSettings getSystemSettings() {
        return systemSettingsRepository.findById((short) 1)
                .orElse(SystemSettings.builder()
                        .maintainence(false)
                        .payinService(true)
                        .payoutService(true)
                        .build());
    }

    private void saveApiLog(Integer userId,String txnId, String request, String response,
                            String service, String serviceApi, String additionalInfo) {
        ApiLog apiLog = ApiLog.builder()
                .userId(userId)
                .txnId(txnId)
                .txnType("PAYIN")
                .request(request)
                .response(response)
                .service(service)
                .serviceApi(serviceApi)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        apiLogRepository.save(apiLog);
    }

    private void savePayinTransaction(AppUser user, String txnId, CreatePaymentLinkDto request,
                                      String api, String status) {
        PayinTransaction transaction = PayinTransaction.builder()
                .userId(user.getUserId())
                .userName(user.getName())
                .merchant(user.getPayingMerchant())
                .txnId(txnId)
                .orderId(request.getOrderId())
                .amount(request.getOrderAmount())
                .status(status)
                .api(api)
                .ipAddress(IPUtils.parseInetAddress(IPUtils.getClientIP(this.request)))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        payinTransactionRepository.save(transaction);
    }

    private String generateTransactionId() {
        SecureRandom random = new SecureRandom();
        int randomNum = 123121 + random.nextInt(990999 - 123121 + 1);
        return String.valueOf(randomNum) + System.currentTimeMillis();
    }

    private int generateRandomNumber(int min, int max) {
        SecureRandom random = new SecureRandom();
        return min + random.nextInt(max - min + 1);
    }

    private String generateRandomStringIDFC() {
        // Simplified - implement as needed
        return "IDF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateIndianMobileNumber() {
        SecureRandom random = new SecureRandom();
        String[] prefixes = {"6", "7", "8", "9"};
        String prefix = prefixes[random.nextInt(prefixes.length)];
        StringBuilder number = new StringBuilder(prefix);
        for (int i = 0; i < 9; i++) {
            number.append(random.nextInt(10));
        }
        return number.toString();
    }

    private Map<String, Double> getRandomLatLong() {
        SecureRandom random = new SecureRandom();
        // India approximate bounds
        double lat = 8.0 + (random.nextDouble() * 29.0); // 8°N to 37°N
        double lon = 68.0 + (random.nextDouble() * 30.0); // 68°E to 98°E

        return Map.of(
                "latitude", lat,
                "longitude", lon
        );
    }

    private String getVimoAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("secretKey", "a5464155073dca2eff67f7846e902521");
            headers.set("saltKey", "18d418caa91b12dde5b19c0e8985d0e7");
            headers.set("encryptdecryptKey", "bd8897086bc3153275425eb38e5217d5");
            headers.set("userId", "3678e7ba-d3e3-47e0-9676-ab379389bc6a");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(VIMO_AUTH_URL, HttpMethod.POST, entity, Map.class);
            Map<String, Object> result = response.getBody();

            return (String) ((Map<String, Object>) result.get("data")).get("token");

        } catch (Exception e) {
            log.error("Error getting Vimo access token", e);
            throw new ServiceException("Failed to get Vimo access token: " + e.getMessage());
        }
    }

    private String getEncryptedData(Map<String, Object> data) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ENCRYPTION_SERVICE_URL + "/encrypt", entity, Map.class);
            Map<String, Object> result = response.getBody();

            return (String) result.get("encryptedText");

        } catch (Exception e) {
            log.error("Error encrypting data", e);
            throw new ServiceException("Failed to encrypt data: " + e.getMessage());
        }
    }

    private String getDecryptedData(String encryptedData) {
        try {
            Map<String, String> request = Map.of("encryptedText", encryptedData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ENCRYPTION_SERVICE_URL + "/decrypt", entity, Map.class);
            Map<String, Object> result = response.getBody();

            return (String) result.get("decryptedData");

        } catch (Exception e) {
            log.error("Error decrypting data", e);
            throw new ServiceException("Failed to decrypt data: " + e.getMessage());
        }
    }

    private String callAirpayEncryptApi(Map<String, Object> postData) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(postData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(AIRPAY_ENCRYPT_URL, entity, Map.class);
            Map<String, Object> result = response.getBody();

            return (String) result.get("data");

        } catch (Exception e) {
            log.error("Error calling Airpay encrypt API", e);
            throw new ServiceException("Failed to call Airpay encrypt API: " + e.getMessage());
        }
    }

//    private MerchantChargeDto getMerchantCharges(String merchantName, BigDecimal amount, String type) {
//        Optional<Merchant> merchantOpt = merchantRepository.findByMerchantName(merchantName);
//
//        BigDecimal merchantCharge = new BigDecimal("1.75");
//        String merchantChargeType = "PERCENTAGE";
//        BigDecimal merchantGst = new BigDecimal("18");
//        BigDecimal merchantTotalCharge = BigDecimal.ZERO;
//
//        if (merchantOpt.isPresent()) {
//            Merchant merchant = merchantOpt.get();
//            // In a real implementation, you'd fetch from merchant_charge_slabs table
//            // This is simplified
//        }
//
//        if ("FLAT".equals(merchantChargeType)) {
//            merchantTotalCharge = merchantCharge;
//        } else if ("PERCENTAGE".equals(merchantChargeType)) {
//            merchantTotalCharge = amount.multiply(merchantCharge).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
//        }
//
//        BigDecimal merchantTotalGst = merchantTotalCharge.multiply(merchantGst).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
//
//        return MerchantChargeDto.builder()
//                .merchantTotalCharge(merchantTotalCharge)
//                .merchantChargeType(merchantChargeType)
//                .merchantTotalGst(merchantTotalGst)
//                .build();
//    }
//
//    private UserChargeDto getUserCharges(Integer userId, BigDecimal amount, BigDecimal merchantTotalCharge, String type) {
//        BigDecimal charge = new BigDecimal("2");
//        BigDecimal adminCharge = new BigDecimal("20");
//        BigDecimal agentCharge = new BigDecimal("20");
//        String chargeType = "PERCENTAGE";
//        BigDecimal gst = new BigDecimal("18");
//
//        BigDecimal totalCharge = BigDecimal.ZERO;
//        BigDecimal adminTotalcharge = BigDecimal.ZERO;
//        BigDecimal agentTotalcharge = BigDecimal.ZERO;
//
//        // Find user charge slabs
//        List<UserChargeSlab> slabs = userChargeSlabRepository.findByUserUserId(userId);
//
//        if (!slabs.isEmpty()) {
//            // Find applicable slab based on amount
//            // This is simplified
//        }
//
//        if ("FLAT".equals(chargeType)) {
//            totalCharge = charge;
//            adminTotalcharge = adminCharge.subtract(merchantTotalCharge);
//            agentTotalcharge = agentCharge;
//        } else if ("PERCENTAGE".equals(chargeType)) {
//            totalCharge = amount.multiply(charge).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
//            adminTotalcharge = amount.multiply(adminCharge).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
//                    .subtract(merchantTotalCharge);
//            agentTotalcharge = amount.multiply(agentCharge).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
//        }
//
//        BigDecimal totalGst = totalCharge.multiply(gst).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
//        BigDecimal adminTax = adminTotalcharge.multiply(gst).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
//        BigDecimal agentTax = agentTotalcharge.multiply(gst).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
//
//        return UserChargeDto.builder()
//                .totalCharge(totalCharge)
//                .adminCharge(adminCharge)
//                .agentCharge(agentCharge)
//                .chargeType(chargeType)
//                .totalGst(totalGst)
//                .adminTotalcharge(adminTotalcharge)
//                .agentTotalcharge(agentTotalcharge)
//                .adminTax(adminTax)
//                .agentTax(agentTax)
//                .build();
//    }
//
//    @Transactional
//    protected void saveTransactionToDB(AppUser user, PayinCallbackDto transaction, Map<String, Object> chargeData) {
//        try {
//            // Create user transaction
//            UserTransaction userTransaction = UserTransaction.builder()
//                    .userId(user.getUserId())
//                    .userName(user.getName())
//                    .txnId((String) chargeData.get("txnId"))
//                    .orderId((String) chargeData.get("orderId"))
//                    .type("CREDIT")
//                    .operator("PAYIN")
//                    .payerAmount((BigDecimal) chargeData.get("payerAmount"))
//                    .callbackReceived(true)
//                    .merchantCharge((BigDecimal) chargeData.get("merchantCharge"))
//                    .merchantAssigned(user.getPayingMerchant().getMerchantName())
//                    .merchantGst((BigDecimal) chargeData.get("merchantGst"))
//                    .adminCharge((BigDecimal) chargeData.get("adminCharge"))
//                    .admintax((BigDecimal) chargeData.get("admintax"))
//                    .agentCharge((BigDecimal) chargeData.get("agentCharge"))
//                    .agenttax((BigDecimal) chargeData.get("agenttax"))
//                    .openBalance((BigDecimal) chargeData.get("openBalance"))
//                    .amount((BigDecimal) chargeData.get("totalAmount"))
//                    .walletBalance((BigDecimal) chargeData.get("closeBalance"))
//                    .closingSettlementBalance((BigDecimal) chargeData.get("settlement"))
//                    .credit((BigDecimal) chargeData.get("totalAmount"))
//                    .debit(BigDecimal.ZERO)
//                    .status("SUCCESS")
//                    .remark("Money Added Via Upi")
//                    .api(transaction.getBank())
//                    .requestIp(IPUtils.getClientIP(request))
//                    .chargeDetails(chargeData.toString())
//                    .createdBy(user.getUserId())
//                    .createdAt(OffsetDateTime.now())
//                    .updatedAt(OffsetDateTime.now())
//                    .build();
//
//            userTransactionRepository.save(userTransaction);
//
//            // Update payin transaction
//            PayinTransaction payinTxn = payinTransactionRepository.findByTxnId((String) chargeData.get("txnId"))
//                    .orElseThrow(() -> new ResourceNotFoundException("Payin transaction not found"));
//
//            payinTxn.setPayerName(transaction.getPayerName());
//            payinTxn.setPayerUpi(transaction.getPayerUpi());
//            payinTxn.setMerchantCharge((BigDecimal) chargeData.get("merchantCharge"));
//            payinTxn.setMerchantGst((BigDecimal) chargeData.get("merchantGst"));
//            payinTxn.setAdminCharge((BigDecimal) chargeData.get("adminCharge"));
//            payinTxn.setAdmintax((BigDecimal) chargeData.get("admintax"));
//            payinTxn.setAgentCharge((BigDecimal) chargeData.get("agentCharge"));
//            payinTxn.setAgenttax((BigDecimal) chargeData.get("agenttax"));
//            payinTxn.setCharge((BigDecimal) chargeData.get("charge"));
//            payinTxn.setGst((BigDecimal) chargeData.get("gst"));
//            payinTxn.setTotalAmount((BigDecimal) chargeData.get("totalAmount"));
//            payinTxn.setUtr(transaction.getUtr());
//            payinTxn.setStatus("SUCCESS");
//
//            payinTransactionRepository.save(payinTxn);
//
//            // Add funds to user wallet
//            userService.addFund(user.getUserId(), (BigDecimal) chargeData.get("totalAmount"));
//
//            saveApiLog((String) chargeData.get("txnId"),
//                    "SAVE_USER_TRANSACTION_" + chargeData.get("txnId"),
//                    chargeData.toString(),
//                    "SAVE_USER_TRANSACTION", transaction.getBank(), null);
//
//        } catch (Exception e) {
//            log.error("Error saving transaction to DB", e);
//            saveApiLog((String) chargeData.get("txnId"),
//                    chargeData.toString(),
//                    e.getMessage(),
//                    "SAVE_TRANSACTION_ERROR",
//                    transaction.getBank(), null);
//        }
//    }
//
//    protected void sendClientsCallbackUrlWebhook(AppUser user, PayinCallbackDto paymentDetails) throws JsonProcessingException {
//        Map<String, Object> payinCallBackData = new HashMap<>();
//        payinCallBackData.put("event", "TRANSACTION_CREDIT");
//        payinCallBackData.put("status", "SUCCESS");
//
//        Map<String, Object> data = new HashMap<>();
//        data.put("order_id", paymentDetails.getOrderId());
//        data.put("reference", paymentDetails.getOrderId());
//        data.put("name", paymentDetails.getPayerName());
//        data.put("payer_UPIID", paymentDetails.getPayerUpi());
//        data.put("amount", paymentDetails.getAmount());
//        data.put("UTR", paymentDetails.getUtr());
//        data.put("payment_mode", "UPI");
//        data.put("remarks", paymentDetails.getStatus() ? "Transaction Successful" : "Transaction Failed");
//        data.put("status", paymentDetails.getStatus() ? "SUCCESS" : "FAILED");
//        data.put("created_at", new Date());
//
//        payinCallBackData.put("data", data);
//
//        try {
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payinCallBackData, headers);
//
//            ResponseEntity<String> response = restTemplate.postForEntity(
//                    user.getPayingApiStatus(), entity, String.class);
//
//            saveApiLog("callback sent to client txnId: " + paymentDetails.getOrderId(),
//                    objectMapper.writeValueAsString(payinCallBackData),
//                    response.getBody(),
//                    "CALLBACK-PAYIN-CLIENT-SENT",
//                    paymentDetails.getBank(), null);
//
//        } catch (Exception e) {
//            log.error("Error sending client callback", e);
//            saveApiLog(paymentDetails.getTxnId(),
//                    objectMapper.writeValueAsString(payinCallBackData),
//                    e.getMessage(),
//                    "CALLBACK-PAYIN-CLIENT-ERROR",
//                    paymentDetails.getBank(), null);
//        }
//    }

    private String formatDate(Date date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return formatter.format(date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
    }
}
