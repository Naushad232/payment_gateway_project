// src/main/java/com/suvikapay/wallet/dto/payin/PayinCallbackDto.java
package com.suvikapay.wallet.dto.payin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayinCallbackDto {
    private String bank;
    private String orderId;
    private String txnId;
    private BigDecimal amount;
    private boolean status;
    private String rrn;
    private String payerName;
    private String payerUpi;
    private String utr;
}