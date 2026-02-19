// src/main/java/com/suvikapay/wallet/controller/PayinController.java
package com.suvikapay.wallet.controller;

import com.suvikapay.wallet.dto.payin.*;
import com.suvikapay.wallet.dto.response.ApiResponse;
import com.suvikapay.wallet.service.payin.PayinService;
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

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payin")
@RequiredArgsConstructor
@Tag(name = "Payin", description = "Payin operations endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class PayinController {

    private final PayinService payinService;
    private final AuthUtil authUtil;

    @Operation(summary = "Generate UPI payment link", description = "Generate payment link for UPI transaction")
    @PostMapping("/generate-upi")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PayinResponseDto>> generatePaymentLink(
            @Valid @RequestBody CreatePaymentLinkDto createPaymentLinkDto,
            HttpServletRequest request) {

        log.info("Generate payment link request: {}", createPaymentLinkDto);
        Integer userId = authUtil.getCurrentUserId();

        PayinResponseDto response = payinService.generatePaymentLink(createPaymentLinkDto, userId, "NESTPAY");
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

//    @Operation(summary = "Check payin transaction status", description = "Check status of a payin transaction")
//    @PostMapping("/check-status")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<ApiResponse<PayinStatusResponseDto>> checkPayinStatus(
//            @Valid @RequestBody PayinStatusRequestDto requestDto,
//            HttpServletRequest request) {
//
//        log.info("Check payin status request: {}", requestDto);
//        Integer userId = authUtil.getCurrentUserId();
//
//        PayinStatusResponseDto response = payinService.checkPayinStatus(requestDto, userId);
//        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
//    }
//
//    @Operation(summary = "Callback endpoint for payin transactions", description = "Webhook for payin callbacks")
//    @PostMapping("/callback")
//    public ResponseEntity<Map<String, Object>> payinCallback(@RequestBody Map<String, Object> payload) {
//        log.info("Payin callback received: {}", payload);
//
//        PayinCallbackDto callbackDto = payinService.processPayinCallback(payload);
//        Map<String, Object> response = payinService.bankWebhookMaster(callbackDto);
//
//        return ResponseEntity.ok(response);
//    }
//
//    @Operation(summary = "Airpay payin callback", description = "Webhook for Airpay payin callbacks")
//    @PostMapping("/airpay-callback")
//    public ResponseEntity<Map<String, Object>> airpayPayinCallback(HttpServletRequest request) {
//        Map<String, Object> payload = (Map<String, Object>) request.getParameterMap();
//        log.info("Airpay callback received: {}", payload);
//
//        PayinCallbackDto callbackDto = PayinCallbackDto.builder()
//                .bank("AIRPAY")
//                .orderId((String) payload.get("TRANSACTIONID"))
//                .amount(new BigDecimal(payload.get("AMOUNT").toString()))
//                .status("SUCCESS".equals(payload.get("TRANSACTIONPAYMENTSTATUS")))
//                .rrn((String) payload.get("RRN"))
//                .payerName((String) payload.get("CUSTOMEREMAIL"))
//                .payerUpi((String) payload.get("CUSTOMERVPA"))
//                .utr((String) payload.get("RRN"))
//                .build();
//
//        Map<String, Object> response = payinService.bankWebhookMaster(callbackDto);
//        return ResponseEntity.ok(response);
//    }
//
//    @Operation(summary = "Royale payin callback", description = "Webhook for Royale payin callbacks")
//    @PostMapping("/royale-callback")
//    public ResponseEntity<Map<String, Object>> royalePayinCallback(@RequestBody Map<String, Object> payload) {
//        log.info("Royale callback received: {}", payload);
//
//        PayinCallbackDto callbackDto = PayinCallbackDto.builder()
//                .bank("ROYALE")
//                .orderId((String) payload.get("txnID"))
//                .amount(new BigDecimal(payload.get("payerAmount").toString()))
//                .status("200".equals(payload.get("status")))
//                .rrn((String) payload.get("BankRRN"))
//                .payerName((String) payload.get("payerName"))
//                .payerUpi((String) payload.get("payerVA"))
//                .utr((String) payload.get("BankRRN"))
//                .build();
//
//        Map<String, Object> response = payinService.bankWebhookMaster(callbackDto);
//        return ResponseEntity.ok(Map.of("result", response));
//    }
//
//    @Operation(summary = "Vimo payin callback", description = "Webhook for Vimo payin callbacks")
//    @PostMapping("/vimo-callback")
//    public ResponseEntity<Map<String, Object>> vimoPayinCallback(@RequestBody Map<String, Object> payload) {
//        log.info("Vimo callback received: {}", payload);
//
//        PayinCallbackDto callbackDto = PayinCallbackDto.builder()
//                .bank("VIMO")
//                .orderId((String) payload.get("merchantRefId"))
//                .amount(new BigDecimal(payload.get("amount").toString()))
//                .status("SUCCESS".equalsIgnoreCase((String) payload.get("txnStatus")))
//                .rrn((String) payload.get("rrn"))
//                .payerName((String) payload.get("payerName"))
//                .payerUpi((String) payload.get("payerVPA"))
//                .utr((String) payload.get("rrn"))
//                .build();
//
//        Map<String, Object> response = payinService.bankWebhookMaster(callbackDto);
//        return ResponseEntity.ok(Map.of(
//                "successStatus", true,
//                "message", "Success",
//                "responseCode", "000"
//        ));
//    }
//
//    @Operation(summary = "IDFC payin callback", description = "Webhook for IDFC payin callbacks")
//    @PostMapping("/idfc-callback")
//    public ResponseEntity<Map<String, Object>> idfcPayinCallback(@RequestBody Map<String, Object> payload) {
//        log.info("IDFC callback received: {}", payload);
//
//        // Save raw response first
//        payinService.saveIdfcResponse(payload);
//
//        PayinCallbackDto callbackDto = PayinCallbackDto.builder()
//                .bank("IDFC")
//                .orderId((String) payload.get("OrgTxnRefId"))
//                .txnId((String) payload.get("OrgTxnRefId"))
//                .amount(new BigDecimal(payload.get("Amount").toString()))
//                .status("Approved".equals(payload.get("ResDesc")))
//                .rrn((String) payload.get("OrgCustRefId"))
//                .payerName((String) payload.get("PayerMobileNumber"))
//                .payerUpi((String) payload.get("PayerVirAddr"))
//                .utr((String) payload.get("OrgCustRefId"))
//                .build();
//
//        Map<String, Object> response = payinService.bankWebhookMaster(callbackDto);
//
//        return ResponseEntity.ok(Map.of(
//                "OperationName", "MerchantStatusUpdateResponse",
//                "TxnId", payload.get("OrgTxnRefId"),
//                "ResCode", payload.get("ResCode"),
//                "ResDesc", payload.get("ResDesc"),
//                "TimeStamp", payload.get("TimeStamp"),
//                "HMAC", payload.get("HMAC")
//        ));
//    }
//
//    @Operation(summary = "Fino payin callback", description = "Webhook for Fino payin callbacks")
//    @PostMapping("/fino-callback")
//    public ResponseEntity<Map<String, Object>> finoPayinCallback(@RequestBody Map<String, Object> payload) {
//        log.info("Fino callback received: {}", payload);
//
//        // Decrypt data first (implementation needed in service)
//        Map<String, Object> decryptedPayload = payinService.decryptFinoCallback(payload);
//
//        PayinCallbackDto callbackDto = PayinCallbackDto.builder()
//                .bank("FINO")
//                .orderId((String) decryptedPayload.get("txnReferance"))
//                .amount(new BigDecimal(decryptedPayload.get("TxnAmt").toString()))
//                .status(0 == (Integer) decryptedPayload.get("TxnStatus"))
//                .rrn((String) decryptedPayload.get("RRN"))
//                .payerName((String) decryptedPayload.get("CustomerName"))
//                .payerUpi((String) decryptedPayload.get("CustomerVPA"))
//                .utr((String) decryptedPayload.get("RRN"))
//                .build();
//
//        Map<String, Object> response = payinService.bankWebhookMaster(callbackDto);
//        return ResponseEntity.ok(response);
//    }
//
//    @Operation(summary = "Digi paypoint callback", description = "Webhook for Digi paypoint payin callbacks")
//    @PostMapping("/digi-callback")
//    public ResponseEntity<Map<String, Object>> digiPayinCallback(@RequestBody Map<String, Object> payload) {
//        log.info("Digi callback received: {}", payload);
//
//        Map<String, Object> data = (Map<String, Object>) payload.get("data");
//        String resultStatus = (String) payload.get("resultStatus");
//
//        PayinCallbackDto callbackDto = PayinCallbackDto.builder()
//                .bank("DIGIPAY")
//                .orderId((String) data.get("refId"))
//                .amount(new BigDecimal(data.get("amount").toString()))
//                .status("SUCCESS".equalsIgnoreCase(resultStatus))
//                .rrn((String) data.get("rrn"))
//                .payerName((String) data.get("payeeName"))
//                .payerUpi((String) data.get("payeeAddress"))
//                .utr((String) data.get("rrn"))
//                .build();
//
//        Map<String, Object> response = payinService.bankWebhookMaster(callbackDto);
//
//        return ResponseEntity.ok(Map.of(
//                "successStatus", true,
//                "message", "Success",
//                "responseCode", "000"
//        ));
//    }
//
//    @Operation(summary = "Client payin callback checker", description = "Endpoint to check client payin callbacks")
//    @PostMapping("/check-client-callback")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<ApiResponse<Object>> checkClientPayinCallback(@RequestBody Map<String, Object> payload) {
//        log.info("Check client payin callback: {}", payload);
//
//        Object response = payinService.checkClientPayinCallback(payload);
//        return ResponseEntity.ok(ApiResponse.success("Callback checked", response));
//    }
//
//    @Operation(summary = "Create chargeback", description = "Create a chargeback for a payin transaction")
//    @PostMapping("/chargeback")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ApiResponse<Object>> createChargeback(@Valid @RequestBody ChargebackRequestDto request) {
//        log.info("Create chargeback request: {}", request);
//
//        payinService.updateChargeback(request.getOrderId(), request.getChargebackBy());
//        return ResponseEntity.ok(ApiResponse.success("Chargeback recorded", request));
//    }
//
//    @Operation(summary = "List chargebacks", description = "Get paginated list of chargebacks")
//    @PostMapping("/chargeback/list")
//    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
//    public ResponseEntity<ApiResponse<Object>> getChargebacks(@Valid @RequestBody ChargebackListRequestDto request) {
//        log.info("List chargebacks request: {}", request);
//
//        Map<String, Object> result = payinService.findChargebacksPaginated(
//                request.getUserId(),
//                request.getType(),
//                request.getStartDate(),
//                request.getEndDate(),
//                request.getStart(),
//                request.getLength()
//        );
//
//        return ResponseEntity.ok(ApiResponse.success("Chargebacks retrieved", result));
//    }
//
//    @Operation(summary = "Get total payin after date", description = "Get total payin amount after a specific date")
//    @GetMapping("/total-after/{userId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
//    public ResponseEntity<ApiResponse<Object>> getTotalPayinAfter(
//            @PathVariable Integer userId,
//            @RequestParam(required = false) String lastSettlementDate,
//            @RequestParam(defaultValue = "false") Boolean total) {
//
//        log.info("Get total payin after for user: {}, date: {}, total: {}", userId, lastSettlementDate, total);
//
//        Map<String, Object> result = payinService.getTotalPayinAfter(userId, lastSettlementDate, total);
//        return ResponseEntity.ok(ApiResponse.success((String) result.get("message"), result));
//    }
}