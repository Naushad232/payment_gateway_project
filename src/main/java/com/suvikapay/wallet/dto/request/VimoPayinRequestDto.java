package com.suvikapay.wallet.dto.request;


import lombok.Data;

@Data
public class VimoPayinRequestDto {
    private String userMobileNo;
    private String merchantRefId;
    private Double amount;
    private Double lat;
    private Double lon;
    private String udf1;
    private String udf2;
    private String udf3;
}
