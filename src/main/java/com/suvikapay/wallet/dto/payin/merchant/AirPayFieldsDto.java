// src/main/java/com/suvikapay/wallet/dto/payin/merchant/AirPayFieldsDto.java
package com.suvikapay.wallet.dto.payin.merchant;

import lombok.Data;

@Data
public class AirPayFieldsDto {
    private String mercid;
    private String orderid;
    private String amount;
    private String buyerPhone;
    private String buyerEmail;
    private String mer_dom;
    private String call_type;
}