package com.suvikapay.wallet.dto.response;

import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.util.List;
@Data
@Builder
public class MerchantResponse {
    private Long merchantId;
    private String merchantName;
    private List  charges;
    @Data
    @Builder
    public static class ChargeSlab {
        private Long slabId;
        private String serviceType;
        private String mode;
        private BigDecimal startAmount;
        private BigDecimal endAmount;
        private BigDecimal charge;
        private String chargeType;
        private BigDecimal gstPercent;
    }
}
