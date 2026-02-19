// src/main/java/com/suvikapay/wallet/service/payin/PayinService.java
package com.suvikapay.wallet.service.payin;

import com.suvikapay.wallet.dto.payin.*;

import java.time.OffsetDateTime;
import java.util.Map;

public interface PayinService {

    PayinResponseDto generatePaymentLink(CreatePaymentLinkDto request, Integer userId, String appName);

//    PayinStatusResponseDto checkPayinStatus(PayinStatusRequestDto request, Integer userId);

//    PayinCallbackDto processPayinCallback(Map<String, Object> payload);

//    Map<String, Object> bankWebhookMaster(PayinCallbackDto callbackDto);

//    Object checkClientPayinCallback(Map<String, Object> payload);

//    void updateChargeback(String orderId, String reason);
//
//    Map<String, Object> findChargebacksPaginated(String userId, String type,
//                                                 OffsetDateTime startDate, OffsetDateTime endDate,
//                                                 Integer start, Integer length);

//    Map<String, Object> getTotalPayinAfter(Integer userId, String lastSettlementDate, Boolean total);

//    Map<String, Object> saveIdfcResponse(Map<String, Object> payload);

//    Map<String, Object> decryptFinoCallback(Map<String, Object> encryptedData);
}