// src/main/java/com/suvikapay/wallet/dto/payout/PayoutCallbackDto.java
package com.suvikapay.wallet.dto.payout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutCallbackDto {
    private String bank;
    private String orderId;
    private String txnId;
    private Double amount;
    private String status;
    private String rrn;
    private String utr;
    private String message;
    private String optxid;
    private String reference;
}