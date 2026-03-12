package com.suvikapay.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChargeSlabResponse {
    private BigDecimal startAmount;
    private BigDecimal endAmount;
    private BigDecimal payinCharge;
    private String payinChargeType;
    private BigDecimal payoutCharge;
    private String payoutChargeType;
    private BigDecimal agentPayinCharge;
    private BigDecimal agentPayoutCharge;
}
