package com.suvikapay.wallet.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserChargeSlabRequest {

    @NotNull(message = "startAmount is required")
    @DecimalMin(value = "0.00", message = "startAmount must be positive")
    private BigDecimal startAmount;

    @NotNull(message = "endAmount is required")
    @DecimalMin(value = "0.00", message = "endAmount must be positive")
    private BigDecimal endAmount;

    private BigDecimal payinCharge;
    private String payinChargeType;

    private BigDecimal payoutCharge;
    private String payoutChargeType;

    private BigDecimal agentPayinCharge;
    private BigDecimal agentPayoutCharge;
}
