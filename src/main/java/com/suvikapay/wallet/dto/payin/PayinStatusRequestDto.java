// src/main/java/com/suvikapay/wallet/dto/payin/PayinStatusRequestDto.java
package com.suvikapay.wallet.dto.payin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayinStatusRequestDto {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    private String bank; // Optional: to check status from specific bank
}