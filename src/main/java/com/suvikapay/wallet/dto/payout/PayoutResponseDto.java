// src/main/java/com/suvikapay/wallet/dto/payout/PayoutResponseDto.java
package com.suvikapay.wallet.dto.payout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutResponseDto {
    private boolean status;
    private boolean error;
    private String message;
    private PayoutDataDto data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayoutDataDto {
        private String payoutRef;
        private String payoutId;
        private Double amount;
        private String rrn;
        private String remark;
        private String status;
    }
}