// src/main/java/com/suvikapay/wallet/dto/request/CreateAdminRequest.java
package com.suvikapay.wallet.dto.request;

import com.suvikapay.wallet.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateAdminRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "Username is required")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]{3,20}$",
            message = "Username must be 3-20 characters and can only contain letters, numbers, and underscores"
    )
    private String userName;

    @ValidPassword
    private String password;

    // Optional fields
    private String userType;
}
