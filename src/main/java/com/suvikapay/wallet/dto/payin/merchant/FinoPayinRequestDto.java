package com.suvikapay.wallet.dto.payin.merchant;



import lombok.Data;

@Data
public class FinoPayinRequestDto {
    private String apiId;
    private String bankId;
    private String amount;
    private String payeeVPA;
    private String mobile;
    private String expiryTime;
    private String txnNote;
    private String txnReferance;
}
