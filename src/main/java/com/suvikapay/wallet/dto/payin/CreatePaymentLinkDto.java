package com.suvikapay.wallet.dto.payin;


import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreatePaymentLinkDto {

    @NotNull(message = "Order amount is required")
    @Positive(message = "Order amount must be positive")
    @DecimalMin(value = "10.00", message = "Amount must be at least 10")
    @DecimalMax(value = "100000.00", message = "Amount must not exceed 100000")
    private BigDecimal orderAmount;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobile;

    @NotBlank(message = "Order ID is required")
    @Size(min = 11, message = "Order ID must be at least 11 characters")
    private String orderId;
}
