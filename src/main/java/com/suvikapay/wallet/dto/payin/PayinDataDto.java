package com.suvikapay.wallet.dto.payin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayinDataDto {
    private String paymentLink;
    private String paymentProcessUrl;
    private String referenceId;
    private String transactionId;
    private String status;
}