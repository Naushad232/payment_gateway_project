package com.suvikapay.wallet.dto.payin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayinStatusResponseDto {
    private Boolean status;
    private Boolean error;
    private String message;
    private PayinStatusDataDto data;
}