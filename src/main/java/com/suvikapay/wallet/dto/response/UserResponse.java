// src/main/java/com/suvikapay/wallet/dto/response/UserResponse.java
package com.suvikapay.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Integer userId;
    private String emailAddress;
    private String userName;
    private String name;
    private String role;
    private String userType;
    private Boolean isActive;
    private String payingApiStatus;
    private String payoutApiStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLogin;
    private MerchantResponse payingMerchant;
    private MerchantResponse payoutMerchant;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantResponse {
        private Long merchantId;
        private String merchantName;
    }
}