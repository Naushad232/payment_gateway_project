// src/main/java/com/suvikapay/wallet/service/payout/impl/PayoutServiceImpl.java
package com.suvikapay.wallet.service.payout.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suvikapay.wallet.dto.payout.PayoutCallbackDto;
import com.suvikapay.wallet.dto.payout.PayoutRequestDto;
import com.suvikapay.wallet.dto.payout.PayoutResponseDto;
import com.suvikapay.wallet.dto.payout.PayoutStatusRequestDto;
import com.suvikapay.wallet.entity.*;
import com.suvikapay.wallet.exception.ResourceNotFoundException;
import com.suvikapay.wallet.exception.ServiceException;
import com.suvikapay.wallet.exception.UnauthorizedException;
import com.suvikapay.wallet.repository.*;
import com.suvikapay.wallet.service.UserService;
import com.suvikapay.wallet.service.payout.PayoutService;
import com.suvikapay.wallet.util.IPUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutServiceImpl implements PayoutService {

    private final PayoutTransactionRepository payoutTransactionRepository;
    private final UserTransactionRepository userTransactionRepository;
    private final ApiLogRepository apiLogRepository;
    private final UserIpRepository userIpRepository;
    private final UserChargeSlabRepository userChargeSlabRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantChargeSlabRepository merchantChargeSlabRepository;
    private final SystemSettingsRepository systemSettingsRepository;
    private final WalletRepository walletRepository;
    private final UserService userService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    @Value("${app.encryption.service.url:https://suvikapay.com/encryption.php}")
    private String encryptionServiceUrl;

    @Value("${royale.member.id:MRP18638893}")
    private String royaleMemberId;

    @Value("${royale.txn.pwd:784921}")
    private String royaleTxnPwd;

    @Value("${royale.payout.url:https://api.myRoyalPay.in/api/Transfer}")
    private String royalePayoutUrl;

    @Value("${vimo.secret.key:a5464155073dca2eff67f7846e902521}")
    private String vimoSecretKey;

    @Value("${vimo.salt.key:18d418caa91b12dde5b19c0e8985d0e7}")
    private String vimoSaltKey;

    @Value("${vimo.encrypt.key:bd8897086bc3153275425eb38e5217d5}")
    private String vimoEncryptKey;

    @Value("${vimo.user.id:3678e7ba-d3e3-47e0-9676-ab379389bc6a}")
    private String vimoUserId;

    @Value("${vimo.auth.url:https://prod.vidual.in/payoutapi/api/Signature/Authorize}")
    private String vimoAuthUrl;

    @Value("${vimo.payout.url:https://prod.vidual.in/payoutapi/api/Payment/payout}")
    private String vimoPayoutUrl;

    @Value("${idfc.client.id:d0491f0b-e468-4adb-84b6-8e079a256689}")
    private String idfcClientId;

    @Value("${idfc.auth.url:https://apiext.idfcfirstbank.com/authorization/oauth2/token}")
    private String idfcAuthUrl;

    @Value("${idfc.payout.url:https://apiext.payments.idfcfirstbank.com/paymenttxns/v1/fundTransfer}")
    private String idfcPayoutUrl;

    @Value("${idfc.secret.hex.key:82516d706c23516d706c65496465632125536b6584354964641341457a6b6585}")
    private String idfcSecretHexKey;

    @Value("${idfc.debit.account:10209552611}")
    private String idfcDebitAccount;

    @Value("${idfc.teller.id:TER25083088004}")
    private String idfcTellerId;

    @Override
    @Transactional
    public PayoutResponseDto processPayout(PayoutRequestDto request, Integer userId) {
        try {
            log.info("Processing payout for user: {}, request: {}", userId, request);

            // Get user
            AppUser user = userService.getAppUserById(userId);

            // Check system settings
            SystemSettings settings = getSystemSettings();

            if (settings.getMaintainence()) {
                return PayoutResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("System is under maintenance. Please wait for update from admin.")
                        .build();
            }

            if (!settings.getPayoutService()) {
                return PayoutResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Payout services are not available at the moment.")
                        .build();
            }

            // Validate amount
            if (request.getAmount() < 100 || request.getAmount() > 49500) {
                return PayoutResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Amount should be between 100 and 49500")
                        .build();
            }

            // Validate reference number
            if (request.getReference().length() < 11 || request.getReference().length() > 19) {
                return PayoutResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Reference number should be between 11 and 19 characters")
                        .build();
            }

            // Check for special characters in reference
            String specialCharRegex = "[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]+";
            if (request.getReference().matches(specialCharRegex)) {
                return PayoutResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Reference No should not have special characters, please try again with different Reference!")
                        .build();
            }

            // Check for duplicate reference
            Optional<UserTransaction> existingTxn = userTransactionRepository.findByOrderId(request.getReference());
            if (existingTxn.isPresent()) {
                return PayoutResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Duplicate Reference No. Please try with a different reference.")
                        .build();
            }

            // Check user status
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                return PayoutResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Your account is inactive. Please contact administrator.")
                        .build();
            }

            // Check payout API status
            if (user.getPayoutApiStatus() == null || !Boolean.TRUE.equals(user.getPayoutApiStatus())) {
                return PayoutResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("We are taking downtime due to some technical issues. Please wait till further update.")
                        .build();
            }

            // IP Whitelist validation
            String clientIp = IPUtils.getClientIP((HttpServletRequest) request);
            InetAddress clientInetAddress = IPUtils.parseInetAddress(clientIp);

            List<UserIp> userIps = userIpRepository.findByUser(user);

            if (!userIps.isEmpty()) {
                boolean ipAllowed = userIps.stream()
                        .anyMatch(ip -> ip.getIpAddress().equals(clientInetAddress));

                if (!ipAllowed) {
                    return PayoutResponseDto.builder()
                            .status(false)
                            .error(true)
                            .message("Your IP is not whitelisted. Requested IP: " + clientIp)
                            .build();
                }
                log.info("IP whitelisted: {}, processing further...", clientIp);
            } else {
                log.info("No IP whitelist configured for user {}, skipping IP validation", user.getUserId());
            }

            // Generate transaction ID
            String txnId = generateTransactionId();

            // Get merchant charges
            Merchant payoutMerchant = user.getPayoutMerchant();
            String merchantName = payoutMerchant != null ? payoutMerchant.getMerchantName() : "";

            Map<String, Object> merchantCharge = getMerchantCharges(
                    merchantName,
                    BigDecimal.valueOf(request.getAmount()),
                    "PAYOUT"
            );

            // Get user charges
            Map<String, Object> userCharge = getUserCharges(
                    userId,
                    BigDecimal.valueOf(request.getAmount()),
                    (BigDecimal) merchantCharge.get("merchantTotalCharge"),
                    "PAYOUT"
            );

            // Calculate totals
            BigDecimal totalCharge = (BigDecimal) userCharge.get("totalCharge");
            BigDecimal totalGst = (BigDecimal) userCharge.get("totalGst");
            BigDecimal totalDeductAmount = BigDecimal.valueOf(request.getAmount())
                    .add(totalCharge)
                    .add(totalGst);

            // Check wallet and settlement balance
            Wallet wallet = walletRepository.findByUser_UserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", userId));

            // Get settlement balance (you might need to add this field to AppUser or Wallet)
            // For now, we'll use wallet balance as settlement balance
            BigDecimal openSettlementBal = wallet.getCurrentBalance();
            BigDecimal rollingReserve = user.getRollingReserve() != null ? user.getRollingReserve() : BigDecimal.ZERO;

            log.info("openSettlementBal: {}, rollingReserve: {}, totalDeductAmount: {}",
                    openSettlementBal, rollingReserve, totalDeductAmount);

            if (openSettlementBal.compareTo(BigDecimal.ZERO) == 0 ||
                    openSettlementBal.compareTo(totalDeductAmount) <= 0) {
                return PayoutResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Insufficient fund")
                        .build();
            }

            if (openSettlementBal.subtract(rollingReserve).compareTo(totalDeductAmount) <= 0) {
                return PayoutResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("Service not available for now. Please try again after sometime or contact Administrator")
                        .build();
            }

            // Create user transaction
            UserTransaction userTransaction = createUserTransaction(user, request, txnId,
                    merchantCharge, userCharge, totalDeductAmount, wallet.getCurrentBalance(),
                    openSettlementBal, clientIp, merchantName);

            // Create payout transaction
            PayoutTransaction payoutTransaction = createPayoutTransaction(user, request, txnId,
                    merchantCharge, userCharge, totalDeductAmount, clientInetAddress, merchantName);

            // Deduct funds from wallet
            wallet.setCurrentBalance(wallet.getCurrentBalance().subtract(totalDeductAmount));
            wallet.setUpdatedAt(OffsetDateTime.now());
            walletRepository.save(wallet);

            // Process with merchant based on assigned merchant
            PayoutResponseDto merchantResponse = processWithMerchant(merchantName, request, txnId);

            // Update transaction status based on merchant response
            if (merchantResponse != null && merchantResponse.getData() != null) {
                updateTransactionWithMerchantResponse(userTransaction, payoutTransaction, merchantResponse);
            }

            // Save API log
            saveApiLog(userId, txnId, request, merchantResponse, "PAYOUT-INITIATE", merchantName);

            return PayoutResponseDto.builder()
                    .status(true)
                    .error(false)
                    .message("Kindly allow some time for the payout to process")
                    .data(PayoutResponseDto.PayoutDataDto.builder()
                            .payoutRef(request.getReference())
                            .payoutId(txnId)
                            .amount(request.getAmount())
                            .rrn("")
                            .remark("Transaction in Queue")
                            .status("SUCCESS")
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Error processing payout", e);
            throw new ServiceException("Failed to process payout: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> payoutCheckStatus(PayoutStatusRequestDto request, Integer userId) {
        try {
            log.info("Checking payout status for reference: {}, userId: {}", request.getReferenceNumber(), userId);

            // Find transaction
            Optional<UserTransaction> userTxnOpt = userTransactionRepository.findByOrderId(request.getReferenceNumber());

            if (userTxnOpt.isEmpty()) {
                return Map.of(
                        "status", false,
                        "error", true,
                        "message", "Transaction not found",
                        "data", Map.of("payout_ref", request.getReferenceNumber())
                );
            }

            UserTransaction userTxn = userTxnOpt.get();

            // Get user to check role
            AppUser loggedUser = userService.getAppUserById(userId);

            // Verify ownership (admin can view all, users can view their own)
            if (!userTxn.getUserId().equals(userId) && !"ADMIN".equals(loggedUser.getRole())) {
                return Map.of(
                        "status", false,
                        "error", true,
                        "message", "Unauthorized to view this transaction"
                );
            }

            // Find payout transaction
            Optional<PayoutTransaction> payoutTxnOpt = payoutTransactionRepository.findByOrderId(request.getReferenceNumber());

            Map<String, Object> data = new HashMap<>();
            data.put("payout_ref", request.getReferenceNumber());

            if (payoutTxnOpt.isPresent()) {
                PayoutTransaction payoutTxn = payoutTxnOpt.get();
                data.put("remark", payoutTxn.getRemark() != null ? payoutTxn.getRemark() : "");
                data.put("status", payoutTxn.getStatus());
                data.put("bank_ref", payoutTxn.getUtr() != null ? payoutTxn.getUtr() : "");
                data.put("amount", payoutTxn.getAmount());
            } else {
                data.put("remark", userTxn.getRemark() != null ? userTxn.getRemark() : "");
                data.put("status", userTxn.getStatus());
                data.put("bank_ref", "");
                data.put("amount", userTxn.getPayerAmount());
            }

            return Map.of(
                    "status", true,
                    "error", false,
                    "message", userTxn.getRemark() != null ? userTxn.getRemark() : "",
                    "data", data
            );

        } catch (Exception e) {
            log.error("Error checking payout status", e);
            return Map.of(
                    "status", false,
                    "error", true,
                    "message", e.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public Map<String, Object> payoutCallback(PayoutCallbackDto callbackDto) {
        try {
            log.info("Processing payout callback: {}", callbackDto);

            Optional<UserTransaction> userTxnOpt = userTransactionRepository.findByOrderId(callbackDto.getReference());

            if (userTxnOpt.isEmpty()) {
                log.warn("Transaction not found for reference: {}", callbackDto.getReference());

                saveCallbackLog(null, callbackDto, "CALLBACK-PAYOUT-TXN-NOT-FOUND");

                return Map.of(
                        "status", true,
                        "error", false,
                        "message", "callback received"
                );
            }

            UserTransaction userTxn = userTxnOpt.get();

            // Check if callback already received
            if (Boolean.TRUE.equals(userTxn.getCallbackReceived())) {
                return Map.of(
                        "status", true,
                        "error", false,
                        "message", "callback already received"
                );
            }

            saveCallbackLog(userTxn.getUserId(), callbackDto, "CALLBACK-PAYOUT");

            AppUser user = userService.getAppUserById(userTxn.getUserId());
            Integer agentId = null; // You'll need to add agent_id to AppUser if needed

            if ("SUCCESS".equalsIgnoreCase(callbackDto.getStatus())) {
                // Update transactions to SUCCESS
                userTxn.setStatus("SUCCESS");
                userTxn.setCallbackReceived(true);
                userTxn.setUpdatedAt(OffsetDateTime.now());
                userTransactionRepository.save(userTxn);

                Optional<PayoutTransaction> payoutTxnOpt = payoutTransactionRepository.findByOrderId(callbackDto.getReference());
                if (payoutTxnOpt.isPresent()) {
                    PayoutTransaction payoutTxn = payoutTxnOpt.get();
                    payoutTxn.setUtr(callbackDto.getRrn());
                    payoutTxn.setStatus("SUCCESS");
                    payoutTxn.setStatusSuccessDate(OffsetDateTime.now());
                    payoutTxn.setRemark(callbackDto.getMessage());
                    payoutTxn.setUpdatedAt(OffsetDateTime.now());
                    payoutTransactionRepository.save(payoutTxn);
                }

            } else if ("FAILED".equalsIgnoreCase(callbackDto.getStatus())) {
                // Refund amount
                refundPayout(userTxn);

                // Update transactions to FAILED
                userTxn.setStatus("FAILED");
                userTxn.setCallbackReceived(true);
                userTxn.setUpdatedAt(OffsetDateTime.now());
                userTransactionRepository.save(userTxn);

                Optional<PayoutTransaction> payoutTxnOpt = payoutTransactionRepository.findByOrderId(callbackDto.getReference());
                if (payoutTxnOpt.isPresent()) {
                    PayoutTransaction payoutTxn = payoutTxnOpt.get();
                    payoutTxn.setStatus("FAILED");
                    payoutTxn.setStatusFailedDate(OffsetDateTime.now());
                    payoutTxn.setRemark(callbackDto.getMessage());
                    payoutTxn.setUpdatedAt(OffsetDateTime.now());
                    payoutTransactionRepository.save(payoutTxn);
                }

                // Refund agent commission if applicable
                if (agentId != null && agentId > 1 && userTxn.getAgentCharge() != null) {
                    // Implement agent refund logic if needed
                }
            }

            // Send callback to client if configured
            if (user.getPayoutCallback() != null && !user.getPayoutCallback().isEmpty()) {
                sendClientCallback(user, userTxn, callbackDto);
            } else {
                saveCallbackLog(userTxn.getUserId(), callbackDto, "CALLBACK-PAYOUT-CLIENT-NOTFOUND");
            }

            return Map.of(
                    "status", true,
                    "error", false,
                    "message", "callback received"
            );

        } catch (Exception e) {
            log.error("Error processing payout callback", e);
            return Map.of(
                    "status", false,
                    "error", true,
                    "message", e.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public Map<String, Object> royalePayoutCallback(Map<String, Object> payload) {
        try {
            log.info("Royale payout callback: {}", payload);

            String reference = (String) payload.get("txnid");
            String status = ((String) payload.get("status")).toLowerCase();
            String rrn = (String) payload.get("rrn");

            PayoutCallbackDto callbackDto = PayoutCallbackDto.builder()
                    .bank("ROYALE")
                    .orderId(reference)
                    .txnId((String) payload.get("optxid"))
                    .amount(payload.get("amount") != null ? ((Number) payload.get("amount")).doubleValue() : null)
                    .status("success".equals(status) || "SUCCESS".equalsIgnoreCase(status) ? "SUCCESS" : "FAILED")
                    .rrn(rrn)
                    .utr(rrn)
                    .message((String) payload.get("message"))
                    .reference(reference)
                    .build();

            return payoutCallback(callbackDto);

        } catch (Exception e) {
            log.error("Error processing Royale payout callback", e);
            return Map.of(
                    "status", false,
                    "error", true,
                    "message", e.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public Map<String, Object> vimoPayoutCallback(Map<String, Object> payload) {
        try {
            log.info("Vimo payout callback: {}", payload);

            String reference = (String) payload.get("txnid");
            String status = ((String) payload.get("status")).toUpperCase();
            String rrn = (String) payload.get("rrn");

            PayoutCallbackDto callbackDto = PayoutCallbackDto.builder()
                    .bank("VIMO")
                    .orderId(reference)
                    .txnId(reference)
                    .amount(payload.get("amount") != null ? ((Number) payload.get("amount")).doubleValue() : null)
                    .status(status)
                    .rrn(rrn)
                    .utr(rrn)
                    .message("Amount deposit request")
                    .reference(reference)
                    .build();

            return payoutCallback(callbackDto);

        } catch (Exception e) {
            log.error("Error processing Vimo payout callback", e);
            return Map.of(
                    "status", false,
                    "error", true,
                    "message", e.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public Map<String, Object> idfcPayoutCallback(Map<String, Object> payload) {
        try {
            log.info("IDFC payout callback: {}", payload);

            String reference = (String) payload.get("OrgTxnRefId");
            // Fix: Convert boolean to String properly
            boolean isApproved = "Approved".equals(payload.get("ResDesc"));
            String status = isApproved ? "SUCCESS" : "FAILED";
            String rrn = (String) payload.get("OrgCustRefId");

            PayoutCallbackDto callbackDto = PayoutCallbackDto.builder()
                    .bank("IDFC")
                    .orderId(reference)
                    .txnId(reference)
                    .amount(payload.get("Amount") != null ? ((Number) payload.get("Amount")).doubleValue() : null)
                    .status(status)
                    .rrn(rrn)
                    .utr(rrn)
                    .message((String) payload.get("ResDesc"))
                    .reference(reference)
                    .build();

            return payoutCallback(callbackDto);

        } catch (Exception e) {
            log.error("Error processing IDFC payout callback", e);
            return Map.of(
                    "status", false,
                    "error", true,
                    "message", e.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public Map<String, Object> updateTransactionForFailed(String reference) {
        try {
            Optional<UserTransaction> userTxnOpt = userTransactionRepository.findByOrderId(reference);

            if (userTxnOpt.isEmpty()) {
                return Map.of(
                        "status", false,
                        "error", true,
                        "message", "Transaction not found"
                );
            }

            UserTransaction userTxn = userTxnOpt.get();

            if (Boolean.TRUE.equals(userTxn.getCallbackReceived())) {
                return Map.of(
                        "status", true,
                        "error", false,
                        "message", "callback received already"
                );
            }

            // Refund amount
            refundPayout(userTxn);

            // Update user transaction
            userTxn.setStatus("FAILED");
            userTxn.setCallbackReceived(true);
            userTxn.setUpdatedAt(OffsetDateTime.now());
            userTransactionRepository.save(userTxn);

            // Update payout transaction
            Optional<PayoutTransaction> payoutTxnOpt = payoutTransactionRepository.findByOrderId(reference);
            if (payoutTxnOpt.isPresent()) {
                PayoutTransaction payoutTxn = payoutTxnOpt.get();
                payoutTxn.setStatus("FAILED");
                payoutTxn.setStatusFailedDate(OffsetDateTime.now());
                payoutTxn.setRemark("Payout Failed instantly");
                payoutTxn.setUpdatedAt(OffsetDateTime.now());
                payoutTransactionRepository.save(payoutTxn);
            }

            return Map.of(
                    "status", true,
                    "error", false,
                    "message", "Transaction updated as failed"
            );

        } catch (Exception e) {
            log.error("Error updating transaction for failed", e);
            return Map.of(
                    "status", false,
                    "error", true,
                    "message", e.getMessage()
            );
        }
    }

    @Override
    public Map<String, Object> sendClientsCallbackUrlWebhook(Map<String, Object> payload) {
        try {
            String referenceNum = (String) payload.get("referenceNumber");

            Optional<UserTransaction> userTxnOpt = userTransactionRepository.findByOrderId(referenceNum);

            if (userTxnOpt.isEmpty()) {
                return Map.of(
                        "status", false,
                        "error", true,
                        "message", "Transaction not found"
                );
            }

            UserTransaction userTxn = userTxnOpt.get();
            AppUser user = userService.getAppUserById(userTxn.getUserId());

            Optional<PayoutTransaction> payoutTxnOpt = payoutTransactionRepository.findByOrderId(referenceNum);

            Map<String, Object> payoutCallbackData = new HashMap<>();
            payoutCallbackData.put("event", "TRANSFER_STATUS_UPDATE");
            payoutCallbackData.put("status", userTxn.getStatus());

            Map<String, Object> data = new HashMap<>();
            data.put("payout_id", userTxn.getOrderId());
            data.put("amount", userTxn.getPayerAmount());
            data.put("remarks", "Amount deposit request");
            data.put("created_at", new Date());
            data.put("payment_mode", "IMPS");
            data.put("transfer_date", new Date());
            data.put("UTR", payoutTxnOpt.map(PayoutTransaction::getUtr).orElse(""));
            data.put("reference", userTxn.getOrderId());
            data.put("status", userTxn.getStatus());
            data.put("message", "Amount deposit request");

            payoutCallbackData.put("data", data);

            // Send webhook
            if (user.getPayoutCallback() != null && !user.getPayoutCallback().isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payoutCallbackData, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(
                        user.getPayoutCallback(), entity, String.class);

                log.info("Client callback sent successfully: {}", response.getStatusCode());

                // Save callback log
                saveClientCallbackLog(userTxn.getUserId(), userTxn.getOrderId(),
                        payoutCallbackData, response.getBody(), "CALLBACK-PAYOUT-CLIENT-SENT");
            }

            return Map.of(
                    "status", true,
                    "error", false,
                    "message", "callback sent to client for reference " + referenceNum
            );

        } catch (Exception e) {
            log.error("Error sending client callback", e);
            return Map.of(
                    "status", false,
                    "error", true,
                    "message", e.getMessage()
            );
        }
    }

    @Override
    public Map<String, Object> userBalanceStatus(Integer userId) {
        try {
            AppUser user = userService.getAppUserById(userId);
            Wallet wallet = walletRepository.findByUser_UserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", userId));

            // You might need to calculate usable balance (settlement - lien - rolling reserve)
            BigDecimal usableBalance = wallet.getCurrentBalance();
            if (user.getRollingReserve() != null) {
                usableBalance = usableBalance.subtract(user.getRollingReserve());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("walletBalance", wallet.getCurrentBalance());
            data.put("usableBalance", usableBalance.compareTo(BigDecimal.ZERO) > 0 ? usableBalance : BigDecimal.ZERO);

            return data;

        } catch (Exception e) {
            log.error("Error fetching user balance", e);
            throw new ServiceException("Failed to fetch user balance: " + e.getMessage());
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

    private String generateTransactionId() {
        SecureRandom random = new SecureRandom();
        int randomNum = 123121 + random.nextInt(990999 - 123121 + 1);
        return String.valueOf(randomNum) + System.currentTimeMillis();
    }

    private Map<String, Object> getMerchantCharges(String merchantName, BigDecimal amount, String type) {
        BigDecimal merchantCharge = new BigDecimal("1.75");
        String merchantChargeType = "PERCENTAGE";
        BigDecimal merchantGst = new BigDecimal("18");
        BigDecimal merchantTotalCharge = BigDecimal.ZERO;

        if (merchantName != null && !merchantName.isEmpty()) {
            Optional<Merchant> merchantOpt = merchantRepository.findByMerchantName(merchantName);

            if (merchantOpt.isPresent()) {
                Merchant merchant = merchantOpt.get();
                List<MerchantChargeSlab> slabs = merchantChargeSlabRepository
                        .findByMerchantMerchantIdAndServiceTypeAndModeAndStartAmountLessThanEqualAndEndAmountGreaterThanEqual(
                                merchant.getMerchantId(),
                                "WALLET",
                                type,
                                amount,
                                amount
                        );

                if (!slabs.isEmpty()) {
                    MerchantChargeSlab slab = slabs.get(0);
                    merchantCharge = slab.getCharge();
                    merchantChargeType = slab.getChargeType();
                    merchantGst = slab.getGstPercent() != null ? slab.getGstPercent() : BigDecimal.ZERO;
                }
            }
        }

        if ("FLAT".equals(merchantChargeType)) {
            merchantTotalCharge = merchantCharge;
        } else if ("PERCENTAGE".equals(merchantChargeType)) {
            merchantTotalCharge = amount.multiply(merchantCharge)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        BigDecimal merchantTotalGst = merchantTotalCharge.multiply(merchantGst)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("merchantTotalCharge", merchantTotalCharge);
        result.put("merchantChargeType", merchantChargeType);
        result.put("merchantTotalGst", merchantTotalGst);
        return result;
    }

    private Map<String, Object> getUserCharges(Integer userId, BigDecimal amount,
                                               BigDecimal merchantTotalCharge, String type) {
        BigDecimal charge = new BigDecimal("2.5");
        BigDecimal adminCharge = new BigDecimal("10");
        BigDecimal agentCharge = BigDecimal.ZERO;
        String chargeType = "PERCENTAGE";
        BigDecimal gst = new BigDecimal("18");

        List<UserChargeSlab> slabs = userChargeSlabRepository
                .findByUserUserIdAndStartAmountLessThanEqualAndEndAmountGreaterThanEqual(
                        userId, amount, amount);

        if (!slabs.isEmpty()) {
            UserChargeSlab slab = slabs.get(0);
            if ("PAYOUT".equals(type)) {
                charge = slab.getPayoutCharge() != null ? slab.getPayoutCharge() : charge;
                chargeType = slab.getPayoutChargeType() != null ? slab.getPayoutChargeType() : chargeType;
                adminCharge = slab.getAgentPayoutCharge() != null ? slab.getAgentPayoutCharge() : adminCharge;
                agentCharge = slab.getAgentPayoutCharge() != null ? slab.getAgentPayoutCharge() : agentCharge;
            }
        }

        BigDecimal totalCharge = BigDecimal.ZERO;
        BigDecimal adminTotalcharge = BigDecimal.ZERO;
        BigDecimal agentTotalcharge = BigDecimal.ZERO;

        if ("FLAT".equals(chargeType)) {
            totalCharge = charge;
            adminTotalcharge = adminCharge.subtract(merchantTotalCharge);
            agentTotalcharge = agentCharge;
        } else if ("PERCENTAGE".equals(chargeType)) {
            totalCharge = amount.multiply(charge).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            adminTotalcharge = amount.multiply(adminCharge).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                    .subtract(merchantTotalCharge);
            agentTotalcharge = amount.multiply(agentCharge).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalGst = totalCharge.multiply(gst).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal adminTax = adminTotalcharge.multiply(gst).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal agentTax = agentTotalcharge.multiply(gst).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("charge", charge);
        result.put("adminCharge", adminCharge);
        result.put("agentCharge", agentCharge);
        result.put("chargeType", chargeType);
        result.put("totalCharge", totalCharge);
        result.put("totalGst", totalGst);
        result.put("adminTotalcharge", adminTotalcharge);
        result.put("agentTotalcharge", agentTotalcharge);
        result.put("adminTax", adminTax);
        result.put("agentTax", agentTax);
        return result;
    }

    private UserTransaction createUserTransaction(AppUser user, PayoutRequestDto request, String txnId,
                                                  Map<String, Object> merchantCharge,
                                                  Map<String, Object> userCharge,
                                                  BigDecimal totalDeductAmount,
                                                  BigDecimal openBal,
                                                  BigDecimal openSettlementBal,
                                                  String clientIp,
                                                  String merchantName) {
        UserTransaction userTransaction = UserTransaction.builder()
                .userId(user.getUserId())
                .userName(user.getName())
                .txnId(txnId)
                .orderId(request.getReference())
                .type("DEBIT")
                .operator("PAYOUT")
                .payerAmount(BigDecimal.valueOf(request.getAmount()))
                .merchantCharge((BigDecimal) merchantCharge.get("merchantTotalCharge"))
                .merchantAssigned(merchantName)
                .merchantGst((BigDecimal) merchantCharge.get("merchantTotalGst"))
                .adminCharge((BigDecimal) userCharge.get("adminTotalcharge"))
                .admintax((BigDecimal) userCharge.get("adminTax"))
                .agentCharge((BigDecimal) userCharge.get("agentTotalcharge"))
                .agenttax((BigDecimal) userCharge.get("agentTax"))
                .openBalance(openBal)
                .amount(totalDeductAmount)
                .walletBalance(openBal) // Will be updated after deduction
                .closingSettlementBalance(openSettlementBal.subtract(totalDeductAmount))
                .credit(BigDecimal.ZERO)
                .debit(totalDeductAmount)
                .status("QUEUED")
                .remark("Money Transfer Via Payout")
                .api(merchantName)
                .requestIp(clientIp)
                .chargeDetails(userCharge.toString())
                .createdBy(user.getUserId())
                .callbackReceived(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        return userTransactionRepository.save(userTransaction);
    }

    private PayoutTransaction createPayoutTransaction(AppUser user, PayoutRequestDto request, String txnId,
                                                      Map<String, Object> merchantCharge,
                                                      Map<String, Object> userCharge,
                                                      BigDecimal totalDeductAmount,
                                                      InetAddress clientIp,
                                                      String merchantName) {
        Merchant payoutMerchant = user.getPayoutMerchant();

        PayoutTransaction payoutTransaction = PayoutTransaction.builder()
                .userId(user.getUserId())
                .userName(user.getName())
                .merchant(payoutMerchant)
                .txnId(txnId)
                .orderId(request.getReference())
                .amount(BigDecimal.valueOf(request.getAmount()))
                .merchantCharge((BigDecimal) merchantCharge.get("merchantTotalCharge"))
                .merchantGst((BigDecimal) merchantCharge.get("merchantTotalGst"))
                .adminCharge((BigDecimal) userCharge.get("adminTotalcharge"))
                .adminTax((BigDecimal) userCharge.get("adminTax"))
                .agentCharge((BigDecimal) userCharge.get("agentTotalcharge"))
                .agentTax((BigDecimal) userCharge.get("agentTax"))
                .charge((BigDecimal) userCharge.get("totalCharge"))
                .gst((BigDecimal) userCharge.get("totalGst"))
                .totalAmount(totalDeductAmount)
                .mode(request.getRequesttype())
                .beneName(request.getBeneficiaryName())
                .beneBank(request.getBankname())
                .beneAccount(request.getAccountNumber())
                .beneIfsc(request.getAccountIfsc())
                .status("QUEUED")
                .ipAddress(clientIp)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        return payoutTransactionRepository.save(payoutTransaction);
    }

    private PayoutResponseDto processWithMerchant(String merchantName, PayoutRequestDto request, String txnId) {
        // Route to appropriate merchant handler
        if (merchantName == null) {
            log.warn("No merchant assigned for payout");
            return null;
        }

        switch (merchantName) {
            case "ROYALE":
                return callRoyalePayout(request);
            case "VIMO":
                return callVimoPayout(request);
            case "IDFC":
                return callIdfcPayout(request);
            default:
                log.warn("No payout handler for merchant: {}", merchantName);
                return null;
        }
    }

    private PayoutResponseDto callRoyalePayout(PayoutRequestDto request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("MemberID", royaleMemberId);
            headers.set("TXNPWD", royaleTxnPwd);

            String mobileNo = generateIndianMobileNumber();

            Map<String, Object> body = new HashMap<>();
            body.put("txnID", request.getReference());
            body.put("amount", request.getAmount());
            body.put("account_holder_name", request.getBeneficiaryName());
            body.put("account_no", request.getAccountNumber());
            body.put("ifsc", request.getAccountIfsc());
            body.put("response_type", 1);
            body.put("mobile", mobileNo);
            body.put("Bankcode", "067");
            body.put("Statecode", "JH");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    royalePayoutUrl, entity, Map.class);

            Map<String, Object> result = response.getBody();

            if (result != null) {
                String status = ((String) result.get("status")).toLowerCase();
                String optxid = (String) result.get("optxid");

                if ("success".equals(status) || "pending".equals(status)) {
                    return PayoutResponseDto.builder()
                            .status(true)
                            .error(false)
                            .message("Kindly allow some time for the payout to process")
                            .data(PayoutResponseDto.PayoutDataDto.builder()
                                    .payoutRef(request.getReference())
                                    .payoutId(optxid)
                                    .amount(request.getAmount())
                                    .rrn(optxid)
                                    .remark("Transaction is in Queue")
                                    .status(status.toUpperCase())
                                    .build())
                            .build();
                } else if ("failed".equals(status)) {
                    return PayoutResponseDto.builder()
                            .status(false)
                            .error(true)
                            .message("Transaction Failed at the moment please check account details")
                            .data(PayoutResponseDto.PayoutDataDto.builder()
                                    .payoutRef(request.getReference())
                                    .payoutId(optxid)
                                    .amount(request.getAmount())
                                    .rrn("")
                                    .remark("Invalid account or bank server issue")
                                    .status("FAILED")
                                    .build())
                            .build();
                }
            }

            return PayoutResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message("FAILED")
                    .build();

        } catch (Exception e) {
            log.error("Error calling Royale payout", e);
            return PayoutResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message(e.getMessage())
                    .build();
        }
    }

    private PayoutResponseDto callVimoPayout(PayoutRequestDto request) {
        try {
            // Get Vimo access token
            String bearerToken = getVimoAccessToken();

            // Prepare data to encrypt
            Map<String, Object> dataToEncrypt = new HashMap<>();
            dataToEncrypt.put("amount", request.getAmount());
            dataToEncrypt.put("merchantRefId", request.getReference());
            dataToEncrypt.put("beneficiaryBank", getBankCode(request.getBankname()));
            dataToEncrypt.put("paymentPurpose", "004");
            dataToEncrypt.put("paymentMode", request.getRequesttype().toLowerCase());
            dataToEncrypt.put("beneficiaryAccountNumber", request.getAccountNumber());
            dataToEncrypt.put("beneficiaryIFSC", request.getAccountIfsc());
            dataToEncrypt.put("beneficiaryMobileNumber", generateIndianMobileNumber());
            dataToEncrypt.put("beneficiaryName", request.getBeneficiaryName());
            dataToEncrypt.put("beneficiaryLocation", "JH");
            dataToEncrypt.put("lat", "28.7041");
            dataToEncrypt.put("long", "77.1025");
            dataToEncrypt.put("udf1", "");
            dataToEncrypt.put("udf2", "");
            dataToEncrypt.put("udf3", "");

            // Encrypt data
            String encryptedPayload = getEncryptedData(dataToEncrypt);

            // Call Vimo payout API
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(bearerToken);
            headers.set("userId", vimoUserId);

            Map<String, Object> requestBody = Map.of("requestBody", encryptedPayload);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(vimoPayoutUrl, entity, Map.class);
            Map<String, Object> encryptedResponse = response.getBody();

            if (encryptedResponse != null && encryptedResponse.containsKey("data")) {
                // Decrypt response
                String decryptedResponse = getDecryptedData((String) encryptedResponse.get("data"));
                Map<String, Object> finalResult = objectMapper.readValue(decryptedResponse, Map.class);

                String txnStatus = (String) finalResult.get("txnStatus");

                if ("Queued".equals(txnStatus)) {
                    return PayoutResponseDto.builder()
                            .status(true)
                            .error(false)
                            .message("Kindly allow some time for the payout to process")
                            .data(PayoutResponseDto.PayoutDataDto.builder()
                                    .payoutRef(request.getReference())
                                    .payoutId((String) finalResult.get("merchantRefId"))
                                    .amount(request.getAmount())
                                    .rrn("")
                                    .remark("Transaction in Queue")
                                    .status("SUCCESS")
                                    .build())
                            .build();
                }
            }

            return PayoutResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message("FAILED")
                    .build();

        } catch (Exception e) {
            log.error("Error calling Vimo payout", e);
            return PayoutResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message(e.getMessage())
                    .build();
        }
    }

    private PayoutResponseDto callIdfcPayout(PayoutRequestDto request) {
        try {
            // Get IDFC access token
            String bearerToken = getIdfcAccessToken();

            // Prepare encrypted payload
            Map<String, Object> fundTransferReq = new HashMap<>();
            fundTransferReq.put("tellerBranch", "");
            fundTransferReq.put("tellerID", idfcTellerId);
            fundTransferReq.put("transactionID", request.getReference());
            fundTransferReq.put("debitAccountNumber", idfcDebitAccount);
            fundTransferReq.put("creditAccountNumber", request.getAccountNumber());
            fundTransferReq.put("remitterName", "Suvika Pay");
            fundTransferReq.put("amount", request.getAmount().toString());
            fundTransferReq.put("currency", "INR");
            fundTransferReq.put("transactionType", request.getRequesttype());
            fundTransferReq.put("paymentDescription", "Narration");
            fundTransferReq.put("beneficiaryIFSC", request.getAccountIfsc());
            fundTransferReq.put("beneficiaryName", request.getBeneficiaryName());
            fundTransferReq.put("beneficiaryAddress", "sector 47 Gurgaon");
            fundTransferReq.put("emailId", "support@suvika.in");
            fundTransferReq.put("mobileNo", generateIndianMobileNumber());

            Map<String, Object> dataToEncrypt = new HashMap<>();
            dataToEncrypt.put("initiateAuthGenericFundTransferAPIReq", fundTransferReq);

            Map<String, Object> encryptRequest = new HashMap<>();
            encryptRequest.put("dataToEncrypt", dataToEncrypt);
            encryptRequest.put("secretHexKey", idfcSecretHexKey);

            String encryptedData = callEncryptionService(encryptRequest);

            HttpHeaders headers = new HttpHeaders();
            headers.set("source", "SVP");
            headers.set("correlationId", request.getReference());
            headers.setContentType(MediaType.valueOf("application/octet-stream"));
            headers.setBearerAuth(bearerToken);

            HttpEntity<String> entity = new HttpEntity<>(encryptedData, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(idfcPayoutUrl, entity, Map.class);
            Map<String, Object> result = response.getBody();

            // Decrypt response
            String decryptedResponse = decryptIdfcResponse(result, idfcSecretHexKey);
            Map<String, Object> idfcResponse = objectMapper.readValue(decryptedResponse, Map.class);

            Map<String, Object> apiResp = (Map<String, Object>) idfcResponse.get("initiateAuthGenericFundTransferAPIResp");
            Map<String, Object> metaData = (Map<String, Object>) apiResp.get("metaData");
            Map<String, Object> resourceData = (Map<String, Object>) apiResp.get("resourceData");

            if ("SUCCESS".equals(metaData.get("status")) && "ACPT".equals(resourceData.get("status"))) {
                return PayoutResponseDto.builder()
                        .status(true)
                        .error(false)
                        .message("Kindly allow some time for the payout to process")
                        .data(PayoutResponseDto.PayoutDataDto.builder()
                                .payoutRef(request.getReference())
                                .payoutId((String) resourceData.get("transactionID"))
                                .amount(request.getAmount())
                                .rrn((String) resourceData.get("transactionReferenceNo"))
                                .remark((String) metaData.get("message"))
                                .status("SUCCESS")
                                .build())
                        .build();
            } else if ("ERROR".equals(metaData.get("status"))) {
                // Trigger failed transaction
                Map<String, Object> callbackPayload = new HashMap<>();
                callbackPayload.put("txnid", request.getReference());
                callbackPayload.put("status", "FAILED");
                callbackPayload.put("amount", request.getAmount());
                callbackPayload.put("rrn", "");
                callbackPayload.put("merchantRefId", request.getReference());
                callbackPayload.put("Message", metaData.get("message"));

                // Send callback asynchronously
                new Thread(() -> {
                    try {
                        PayoutCallbackDto callbackDto = PayoutCallbackDto.builder()
                                .bank("IDFC")
                                .orderId(request.getReference())
                                .txnId(request.getReference())
                                .amount(request.getAmount())
                                .status("FAILED")
                                .rrn("")
                                .message((String) metaData.get("message"))
                                .reference(request.getReference())
                                .build();
                        payoutCallback(callbackDto);
                    } catch (Exception ex) {
                        log.error("Error sending failed callback", ex);
                    }
                }).start();

                return PayoutResponseDto.builder()
                        .status(false)
                        .error(true)
                        .message("FAILED")
                        .data(PayoutResponseDto.PayoutDataDto.builder()
                                .payoutRef(request.getReference())
                                .amount(request.getAmount())
                                .rrn("")
                                .remark((String) metaData.get("message"))
                                .status("FAILED")
                                .build())
                        .build();
            }

            return PayoutResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message("FAILED")
                    .build();

        } catch (Exception e) {
            log.error("Error calling IDFC payout", e);
            return PayoutResponseDto.builder()
                    .status(false)
                    .error(true)
                    .message(e.getMessage())
                    .build();
        }
    }

    private void updateTransactionWithMerchantResponse(UserTransaction userTxn,
                                                       PayoutTransaction payoutTxn,
                                                       PayoutResponseDto merchantResponse) {
        if (merchantResponse != null && merchantResponse.getData() != null) {
            if ("FAILED".equals(merchantResponse.getData().getStatus())) {
                // Refund and mark as failed
                refundPayout(userTxn);

                userTxn.setStatus("FAILED");
                userTxn.setRemark(merchantResponse.getData().getRemark());
                userTxn.setUpdatedAt(OffsetDateTime.now());
                userTransactionRepository.save(userTxn);

                payoutTxn.setStatus("FAILED");
                payoutTxn.setStatusFailedDate(OffsetDateTime.now());
                payoutTxn.setRemark(merchantResponse.getData().getRemark());
                payoutTxn.setUpdatedAt(OffsetDateTime.now());
                payoutTransactionRepository.save(payoutTxn);
            }
        }
    }

    private void refundPayout(UserTransaction userTxn) {
        try {
            Wallet wallet = walletRepository.findByUser_UserId(userTxn.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", userTxn.getUserId()));

            BigDecimal refundAmount = userTxn.getDebit() != null ? userTxn.getDebit() : BigDecimal.ZERO;
            wallet.setCurrentBalance(wallet.getCurrentBalance().add(refundAmount));
            wallet.setUpdatedAt(OffsetDateTime.now());
            walletRepository.save(wallet);

            log.info("Refunded {} to user {}", refundAmount, userTxn.getUserId());

        } catch (Exception e) {
            log.error("Error refunding payout", e);
        }
    }

    private void saveApiLog(Integer userId, String txnId, PayoutRequestDto request,
                            PayoutResponseDto response, String service, String serviceApi) {
        try {
            ApiLog apiLog = ApiLog.builder()
                    .userId(userId)
                    .txnId(txnId)
                    .txnType("PAYOUT")
                    .service(service)
                    .serviceApi(serviceApi)
                    .request(objectMapper.writeValueAsString(request))
                    .response(objectMapper.writeValueAsString(response))
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            apiLogRepository.save(apiLog);

        } catch (JsonProcessingException e) {
            log.error("Error saving API log", e);
        }
    }

    private void saveCallbackLog(Integer userId, PayoutCallbackDto callbackDto, String service) {
        try {
            ApiLog apiLog = ApiLog.builder()
                    .userId(userId != null ? userId : 0)
                    .txnId(callbackDto.getReference())
                    .txnType("PAYOUT")
                    .service(service)
                    .serviceApi(callbackDto.getBank())
                    .response(objectMapper.writeValueAsString(callbackDto))
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            apiLogRepository.save(apiLog);

        } catch (JsonProcessingException e) {
            log.error("Error saving callback log", e);
        }
    }

    private void saveClientCallbackLog(Integer userId, String orderId,
                                       Map<String, Object> request, String response, String service) {
        try {
            ApiLog apiLog = ApiLog.builder()
                    .userId(userId)
                    .txnId(orderId)
                    .txnType("PAYOUT")
                    .service(service)
                    .request(objectMapper.writeValueAsString(request))
                    .response(response)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            apiLogRepository.save(apiLog);

        } catch (JsonProcessingException e) {
            log.error("Error saving client callback log", e);
        }
    }

    private void sendClientCallback(AppUser user, UserTransaction userTxn, PayoutCallbackDto callbackDto) {
        try {
            Map<String, Object> payoutCallbackData = new HashMap<>();
            payoutCallbackData.put("event", "TRANSFER_STATUS_UPDATE");
            payoutCallbackData.put("status", userTxn.getStatus());

            Map<String, Object> data = new HashMap<>();
            data.put("payout_id", userTxn.getOrderId());
            data.put("amount", userTxn.getPayerAmount());
            data.put("remarks", "Amount deposit request");
            data.put("created_at", new Date());
            data.put("payment_mode", "IMPS");
            data.put("transfer_date", new Date());
            data.put("UTR", callbackDto.getRrn());
            data.put("reference", userTxn.getOrderId());
            data.put("status", userTxn.getStatus());
            data.put("message", "Amount deposit request");

            payoutCallbackData.put("data", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payoutCallbackData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    user.getPayoutCallback(), entity, String.class);

            log.info("Client callback sent successfully: {}", response.getStatusCode());

            saveClientCallbackLog(user.getUserId(), userTxn.getOrderId(),
                    payoutCallbackData, response.getBody(), "CALLBACK-PAYOUT-CLIENT-SENT");

        } catch (Exception e) {
            log.error("Error sending client callback", e);
            try {
                saveClientCallbackLog(user.getUserId(), userTxn.getOrderId(),
                        Map.of("error", "Failed to send callback"),
                        e.getMessage(), "CALLBACK-PAYOUT-CLIENT-ERROR");
            } catch (Exception ex) {
                log.error("Error logging callback failure", ex);
            }
        }
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

    private String getBankCode(String bankName) {
        // Implement bank code mapping
        Map<String, String> bankCodes = new HashMap<>();
        bankCodes.put("HDFC", "HDFC");
        bankCodes.put("ICICI", "ICICI");
        bankCodes.put("SBI", "SBIN");
        bankCodes.put("AXIS", "UTIB");
        bankCodes.put("KOTAK", "KKBK");
        bankCodes.put("YES", "YESB");
        bankCodes.put("PNB", "PUNB");
        bankCodes.put("CANARA", "CNRB");
        bankCodes.put("BANK OF BARODA", "BARB");
        bankCodes.put("UNION BANK", "UBIN");

        return bankCodes.getOrDefault(bankName.toUpperCase(), "HDFC");
    }

    private String getVimoAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("secretKey", vimoSecretKey);
            headers.set("saltKey", vimoSaltKey);
            headers.set("encryptdecryptKey", vimoEncryptKey);
            headers.set("userId", vimoUserId);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    vimoAuthUrl,
                    HttpMethod.POST, entity, Map.class);

            Map<String, Object> result = response.getBody();
            if (result != null && result.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                return (String) data.get("token");
            }

            throw new ServiceException("Failed to get Vimo access token");

        } catch (Exception e) {
            log.error("Error getting Vimo access token", e);
            throw new ServiceException("Failed to get Vimo access token: " + e.getMessage());
        }
    }

    private String getIdfcAccessToken() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Generate JWT token for IDFC (you'll need to implement this)
            String jwToken = generateIdfcJwtToken();

            String body = "grant_type=client_credentials&" +
                    "scope=paymenttxn-v1fundTransfer paymentenq-paymentTransactionStatus&" +
                    "client_id=" + idfcClientId + "&" +
                    "client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer&" +
                    "client_assertion=" + jwToken;

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(idfcAuthUrl, entity, Map.class);

            Map<String, Object> result = response.getBody();
            if (result != null && result.containsKey("access_token")) {
                return (String) result.get("access_token");
            }

            throw new ServiceException("Failed to get IDFC access token");

        } catch (Exception e) {
            log.error("Error getting IDFC access token", e);
            throw new ServiceException("Failed to get IDFC access token: " + e.getMessage());
        }
    }

    private String generateIdfcJwtToken() {
        // Implement IDFC JWT token generation
        // This is complex and requires private key signing
        // For now, return empty string - you'll need to implement this properly
        log.warn("IDFC JWT token generation not implemented");
        return "";
    }

    private String getEncryptedData(Map<String, Object> data) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(data, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    encryptionServiceUrl + "/encrypt", entity, Map.class);

            Map<String, Object> result = response.getBody();
            if (result != null && result.containsKey("encryptedText")) {
                return (String) result.get("encryptedText");
            }

            throw new ServiceException("Failed to encrypt data");

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
                    encryptionServiceUrl + "/decrypt", entity, Map.class);

            Map<String, Object> result = response.getBody();
            if (result != null && result.containsKey("decryptedData")) {
                return (String) result.get("decryptedData");
            }

            throw new ServiceException("Failed to decrypt data");

        } catch (Exception e) {
            log.error("Error decrypting data", e);
            throw new ServiceException("Failed to decrypt data: " + e.getMessage());
        }
    }

    private String callEncryptionService(Map<String, Object> request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://65.1.212.67:8080/api/encryption/encrypt",
                    entity, String.class);

            return response.getBody();

        } catch (Exception e) {
            log.error("Error calling encryption service", e);
            throw new ServiceException("Failed to encrypt data: " + e.getMessage());
        }
    }

    private String decryptIdfcResponse(Map<String, Object> encryptedResponse, String secretHexKey) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("encryptedData", encryptedResponse);
            request.put("secretHexKey", secretHexKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "http://65.1.212.67:8080/api/encryption/decrypt",
                    entity, String.class);

            return response.getBody();

        } catch (Exception e) {
            log.error("Error decrypting IDFC response", e);
            throw new ServiceException("Failed to decrypt IDFC response: " + e.getMessage());
        }
    }
}