package com.suvikapay.wallet.dto.payin;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PayinCallbackDto {
    private String bank;
    private String orderId;
    private String txnId;
    private BigDecimal amount;
    private Boolean status;
    private String rrn;
    private String payerName;
    private String payerUpi;
    private String utr;
}