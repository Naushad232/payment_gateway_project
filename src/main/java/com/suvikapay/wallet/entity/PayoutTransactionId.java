package com.suvikapay.wallet.entity;

import lombok.*;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class PayoutTransactionId implements Serializable {
    private OffsetDateTime createdAt;
    private Integer userId;
    private String txnId;
}
