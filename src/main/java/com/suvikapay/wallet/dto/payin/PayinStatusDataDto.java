package com.suvikapay.wallet.dto.payin;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Data
@Builder
public class PayinStatusDataDto {
    private String status;
    private String payinRef;
    private String bankRef;
    private BigDecimal amount;
}
