// src/main/java/com/suvikapay/wallet/dto/payin/MerchantChargeDto.java
package com.suvikapay.wallet.dto.payin;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class MerchantChargeDto {
    private BigDecimal merchantTotalCharge;
    private String merchantChargeType;
    private BigDecimal merchantTotalGst;
}