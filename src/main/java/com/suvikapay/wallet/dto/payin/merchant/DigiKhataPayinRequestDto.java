// src/main/java/com/suvikapay/wallet/dto/payin/merchant/DigiKhataPayinRequestDto.java
package com.suvikapay.wallet.dto.payin.merchant;

import lombok.Data;

@Data
public class DigiKhataPayinRequestDto {
    private String orderId;
    private String remarks;
    private String collectExpiryAfter;
    private Double amount;
    private Double latitude;
    private Double longitude;
    private String location;
    private String ipAddress;
    private String deviceSerial;
    private String deviceOS;
    private String appTechName;
}