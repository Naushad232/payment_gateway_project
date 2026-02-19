// src/main/java/com/suvikapay/wallet/dto/payin/ChargeCalculationDto.java
package com.suvikapay.wallet.dto.payin;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ChargeCalculationDto {
    private BigDecimal totalCharge;
    private BigDecimal adminCharge;
    private BigDecimal agentCharge;
    private String chargeType;
    private BigDecimal totalGst;
    private BigDecimal adminTotalcharge;
    private BigDecimal agentTotalcharge;
    private BigDecimal adminTax;
    private BigDecimal agentTax;
    private BigDecimal merchantTotalCharge;
    private BigDecimal merchantTotalGst;
}