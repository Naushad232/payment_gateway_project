package com.suvikapay.wallet.dto.request;

import com.suvikapay.wallet.validation.ValidPassword;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserPartialRequest {

    private String name;

    @Email(message = "Email should be valid")
    private String email;

    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$",
            message = "Username must be 3-20 characters and can only contain letters, numbers, and underscores")
    private String userName;

    @ValidPassword
    private String password;

    private String role;

    private String userType;
    private Long payingMerchantId;
    private Long payoutMerchantId;
    private Boolean payingApiStatus;
    private Boolean payoutApiStatus;
    private Boolean isActive;
    private String payinCallback;
    private String payoutCallback;

    @DecimalMin(value = "0.00", message = "Rolling reserve must be positive")
    private BigDecimal rollingReserve;

    @Valid
    private List<UserChargeSlabRequest> userChargeSlabs;

    @Valid
    private List<@Pattern(
            regexp = "^(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)){3}$",
            message = "Invalid IPv4 address format") String> ipAddresses;
}
