package com.suvikapay.wallet.dto.payin;



import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class ChargebackListRequestDto {
    private String userId;
    private String type;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private Integer start = 0;
    private Integer length = 10;
}