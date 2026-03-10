// src/main/java/com/suvikapay/wallet/dto/payout/PayoutStatusRequestDto.java
package com.suvikapay.wallet.dto.payout;

import lombok.Data;

@Data
public class PayoutStatusRequestDto {
    private String referenceNumber;
    private Boolean callbackDemand;
}