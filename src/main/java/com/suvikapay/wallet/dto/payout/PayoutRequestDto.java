// src/main/java/com/suvikapay/wallet/dto/payout/PayoutRequestDto.java
package com.suvikapay.wallet.dto.payout;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PayoutRequestDto {

    @NotBlank(message = "Reference number is required")
    @Size(min = 11, max = 19, message = "Reference number must be between 11 and 19 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Reference number should not have special characters")
    private String reference;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^\\d{9,18}$", message = "Invalid bank account number format. It should be 9-18 digits.")
    private String accountNumber;

    @NotBlank(message = "Beneficiary name is required")
    private String beneficiaryName;

    @NotBlank(message = "Request type is required")
    private String requesttype; // IMPS, NEFT, RTGS

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "100.00", message = "Amount must be at least 100")
    @DecimalMax(value = "49500.00", message = "Amount must not exceed 49500")
    private Double amount;

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format. It should be like ABCD0XXXXXX.")
    private String accountIfsc;

    @NotBlank(message = "Bank name is required")
    private String bankname;
}