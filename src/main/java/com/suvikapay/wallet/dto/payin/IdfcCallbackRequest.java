package com.suvikapay.wallet.dto.payin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "IDFC Payin Callback Request Payload")
public class IdfcCallbackRequest {

    @Schema(description = "Original Transaction Reference ID",
            example = "TXN123456789",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String OrgTxnRefId;

    @Schema(description = "Transaction Amount",
            example = "1500.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String Amount;

    @Schema(description = "Response Code returned by IDFC",
            example = "00")
    private String ResCode;

    @Schema(description = "Response Description (Approved/Failed)",
            example = "Approved")
    private String ResDesc;

    @Schema(description = "Customer Reference / UTR Number",
            example = "UTR987654321")
    private String OrgCustRefId;

    @Schema(description = "Payer Mobile Number",
            example = "9876543210")
    private String PayerMobileNumber;

    @Schema(description = "Payer Virtual UPI Address",
            example = "payer@idfc")
    private String PayerVirAddr;

    @Schema(description = "Transaction Timestamp",
            example = "2026-02-27T12:30:00")
    private String TimeStamp;

    @Schema(description = "HMAC Signature for verification",
            example = "abc123xyz456")
    private String HMAC;
}