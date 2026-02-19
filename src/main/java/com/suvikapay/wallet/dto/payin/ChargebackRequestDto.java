package com.suvikapay.wallet.dto.payin;




import lombok.Data;

@Data
public class ChargebackRequestDto {
    private String orderId;
    private String user;
    private Double amount;
    private String chargebackBy;
}