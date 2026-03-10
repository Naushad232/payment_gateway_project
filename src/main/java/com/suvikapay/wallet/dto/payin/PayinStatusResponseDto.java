// src/main/java/com/suvikapay/wallet/dto/payin/PayinStatusResponseDto.java
package com.suvikapay.wallet.dto.payin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayinStatusResponseDto {

    private boolean status;
    private boolean error;
    private String message;
    private Integer responseCode;

    private String orderId;
    private String txnId;
    private String bank;
    private BigDecimal amount;
    private String transactionStatus;
    private String utr;
    private String payerName;
    private String payerUpi;
    private OffsetDateTime transactionDate;
    private String failureReason;

    // Additional fields for detailed response
    private BigDecimal charge;
    private BigDecimal gst;
    private BigDecimal totalAmount;
    private String merchantName;
}