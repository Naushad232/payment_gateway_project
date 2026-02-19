// src/main/java/com/suvikapay/wallet/dto/payin/merchant/AirPayEncryptRequestDto.java
package com.suvikapay.wallet.dto.payin.merchant;

import lombok.Data;

@Data
public class AirPayEncryptRequestDto {
    private String encData;
    private String checksum;
    private String mercid;
}