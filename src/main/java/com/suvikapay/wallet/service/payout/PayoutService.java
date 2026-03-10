// src/main/java/com/suvikapay/wallet/service/payout/PayoutService.java
package com.suvikapay.wallet.service.payout;

import com.suvikapay.wallet.dto.payout.PayoutRequestDto;
import com.suvikapay.wallet.dto.payout.PayoutResponseDto;
import com.suvikapay.wallet.dto.payout.PayoutStatusRequestDto;
import com.suvikapay.wallet.dto.payout.PayoutCallbackDto;

import java.util.Map;

public interface PayoutService {

    PayoutResponseDto processPayout(PayoutRequestDto request, Integer userId);

    Map<String, Object> payoutCheckStatus(PayoutStatusRequestDto request, Integer userId);

    Map<String, Object> payoutCallback(PayoutCallbackDto callbackDto);

    Map<String, Object> royalePayoutCallback(Map<String, Object> payload);

    Map<String, Object> vimoPayoutCallback(Map<String, Object> payload);

    Map<String, Object> idfcPayoutCallback(Map<String, Object> payload);

    Map<String, Object> updateTransactionForFailed(String reference);

    Map<String, Object> sendClientsCallbackUrlWebhook(Map<String, Object> payload);
    // Add this method
    Map<String, Object> userBalanceStatus(Integer userId);
}