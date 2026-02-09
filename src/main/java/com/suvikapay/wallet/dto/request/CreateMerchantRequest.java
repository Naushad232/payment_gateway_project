//package com.suvikapay.wallet.dto.request;
//
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotEmpty;
//import jakarta.validation.constraints.NotNull;
//import lombok.Data;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//public class CreateMerchantRequest {
//    // Optional. If not provided, serviceType will default to "WALLET" in the service.
//    private String serviceType;
//
//    @NotEmpty
//    private List<ChargeSlabRequest> charges;
//
//    @Data
//    public static class ChargeSlabRequest {
//        @NotNull
//        private BigDecimal startAmt;
//
//        @NotNull
//        private BigDecimal endAmt;
//
//        @NotNull
//        private BigDecimal charge;
//
//        // e.g., "payin" or "payout"
//        @NotBlank
//        private String mode;
//
//        // e.g., "percentage" or "flat"
//        @NotBlank
//        private String type;
//
//        // Optional GST percent
//        private BigDecimal gstPercent;
//    }
//}
package com.suvikapay.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateMerchantRequest {

    @NotBlank
    private String merchantName;

    // optional (your service defaults to WALLET)
    private String serviceType;

    @NotEmpty
    private List<ChargeRequest> charges;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChargeRequest {
        @NotNull private BigDecimal startAmt;
        @NotNull private BigDecimal endAmt;
        @NotNull private BigDecimal charge;

        @NotBlank private String mode; // payin / payout
        @NotBlank private String type; // percentage / flat

        private BigDecimal gstPercent; // optional
    }
}
