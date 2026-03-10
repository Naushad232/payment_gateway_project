// src/main/java/com/suvikapay/wallet/controller/PayoutController.java
package com.suvikapay.wallet.controller;

import com.suvikapay.wallet.dto.payout.PayoutCallbackDto;
import com.suvikapay.wallet.dto.payout.PayoutRequestDto;
import com.suvikapay.wallet.dto.payout.PayoutStatusRequestDto;
import com.suvikapay.wallet.dto.response.ApiResponse;
import com.suvikapay.wallet.service.payout.PayoutService;
import com.suvikapay.wallet.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payout")
@RequiredArgsConstructor
@Tag(name = "Payout", description = "Payout operations endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class PayoutController {

    private final PayoutService payoutService;
    private final AuthUtil authUtil;

    @Operation(summary = "Process payout", description = "Initiate a payout/withdrawal request")
    @PostMapping("/do-payout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Object>> processPayout(
            @Valid @RequestBody PayoutRequestDto request,
            HttpServletRequest httpRequest) {

        log.info("Process payout request: {}", request);
        Integer userId = authUtil.getCurrentUserId();

        var response = payoutService.processPayout(request, userId);

        if (response.isError()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(response.getMessage()));
        }

        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response.getData()));
    }

    @Operation(summary = "Check payout status", description = "Check status of a payout transaction")
    @PostMapping("/check-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payoutCheckStatus(
            @Valid @RequestBody PayoutStatusRequestDto request,
            HttpServletRequest httpRequest) {

        log.info("Check payout status request: {}", request);
        Integer userId = authUtil.getCurrentUserId();

        Map<String, Object> response = payoutService.payoutCheckStatus(request, userId);

        return ResponseEntity.ok(ApiResponse.success("Status retrieved", response));
    }

    @Operation(summary = "Payout callback", description = "Webhook for payout callbacks")
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> payoutCallback(@RequestBody Map<String, Object> payload) {
        log.info("Payout callback received: {}", payload);

        PayoutCallbackDto callbackDto = PayoutCallbackDto.builder()
                .bank("NESTPAY")
                .orderId(payload.containsKey("data") ?
                        (String) ((Map<String, Object>) payload.get("data")).get("order_id") : null)
                .txnId(payload.containsKey("data") ?
                        (String) ((Map<String, Object>) payload.get("data")).get("payoutId") : null)
                .amount(payload.containsKey("data") ?
                        (Double) ((Map<String, Object>) payload.get("data")).get("amount") : null)
                .status((String) payload.get("status"))
                .rrn(payload.containsKey("data") ?
                        (String) ((Map<String, Object>) payload.get("data")).get("UTR") : null)
                .reference(payload.containsKey("data") ?
                        (String) ((Map<String, Object>) payload.get("data")).get("reference") : null)
                .message(payload.containsKey("data") ?
                        (String) ((Map<String, Object>) payload.get("data")).get("remarks") : null)
                .build();

        Map<String, Object> response = payoutService.payoutCallback(callbackDto);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Royale payout callback", description = "Webhook for Royale payout callbacks")
    @PostMapping("/royale-callback")
    public ResponseEntity<Map<String, Object>> royalePayoutCallback(@RequestBody Map<String, Object> payload) {
        log.info("Royale payout callback received: {}", payload);

        Map<String, Object> response = payoutService.royalePayoutCallback(payload);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Vimo payout callback", description = "Webhook for Vimo payout callbacks")
    @PostMapping("/vimo-callback")
    public ResponseEntity<Map<String, Object>> vimoPayoutCallback(@RequestBody Map<String, Object> payload) {
        log.info("Vimo payout callback received: {}", payload);

        Map<String, Object> callbackPayload = new HashMap<>();
        callbackPayload.put("status", payload.get("txnStatus"));
        callbackPayload.put("rrn", payload.get("rrn"));
        callbackPayload.put("txnid", payload.get("merchantRefId"));
        callbackPayload.put("amount", payload.get("amount"));

        Map<String, Object> response = payoutService.vimoPayoutCallback(callbackPayload);

        return ResponseEntity.ok(Map.of(
                "successStatus", true,
                "message", "Success",
                "responseCode", "000"
        ));
    }

    @Operation(summary = "IDFC payout callback", description = "Webhook for IDFC payout callbacks")
    @PostMapping("/idfc-callback")
    public ResponseEntity<Map<String, Object>> idfcPayoutCallback(@RequestBody Map<String, Object> payload) {
        log.info("IDFC payout callback received: {}", payload);

        Map<String, Object> callbackPayload = new HashMap<>();
        callbackPayload.put("OrgTxnRefId", payload.get("OrgTxnRefId"));
        callbackPayload.put("Amount", payload.get("Amount"));
        callbackPayload.put("ResDesc", payload.get("ResDesc"));
        callbackPayload.put("OrgCustRefId", payload.get("OrgCustRefId"));
        callbackPayload.put("PayerMobileNumber", payload.get("PayerMobileNumber"));
        callbackPayload.put("PayerVirAddr", payload.get("PayerVirAddr"));

        Map<String, Object> response = payoutService.idfcPayoutCallback(callbackPayload);

        return ResponseEntity.ok(Map.of(
                "OperationName", "MerchantStatusUpdateResponse",
                "TxnId", payload.get("OrgTxnRefId"),
                "ResCode", payload.get("ResCode"),
                "ResDesc", payload.get("ResDesc"),
                "TimeStamp", payload.get("TimeStamp"),
                "HMAC", payload.get("HMAC")
        ));
    }

    @Operation(summary = "User balance status", description = "Get current user wallet balance")
    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> userBalanceStatus(HttpServletRequest httpRequest) {
        Integer userId = authUtil.getCurrentUserId();

        Map<String, Object> response = payoutService.userBalanceStatus(userId);

        return ResponseEntity.ok(ApiResponse.success("Balance fetched successfully", response));
    }
}