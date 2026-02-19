// src/main/java/com/suvikapay/wallet/entity/UserTransaction.java
package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "txn_id", nullable = false)
    private String txnId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "operator")
    private String operator;

    @Column(name = "payer_amount", precision = 18, scale = 2)
    private BigDecimal payerAmount;

    @Column(name = "callback_received")
    private Boolean callbackReceived;

    @Column(name = "merchant_charge", precision = 18, scale = 2)
    private BigDecimal merchantCharge;

    @Column(name = "merchant_assigned")
    private String merchantAssigned;

    @Column(name = "merchant_gst", precision = 18, scale = 2)
    private BigDecimal merchantGst;

    @Column(name = "admin_charge", precision = 18, scale = 2)
    private BigDecimal adminCharge;

    @Column(name = "admintax", precision = 18, scale = 2)
    private BigDecimal admintax;

    @Column(name = "agent_charge", precision = 18, scale = 2)
    private BigDecimal agentCharge;

    @Column(name = "agenttax", precision = 18, scale = 2)
    private BigDecimal agenttax;

    @Column(name = "open_balance", precision = 18, scale = 2)
    private BigDecimal openBalance;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "wallet_balance", precision = 18, scale = 2)
    private BigDecimal walletBalance;

    @Column(name = "closing_settlement_balance", precision = 18, scale = 2)
    private BigDecimal closingSettlementBalance;

    @Column(name = "credit", precision = 18, scale = 2)
    private BigDecimal credit;

    @Column(name = "debit", precision = 18, scale = 2)
    private BigDecimal debit;

    @Column(name = "status")
    private String status;

    @Column(name = "remark")
    private String remark;

    @Column(name = "api")
    private String api;

    @Column(name = "request_ip")
    private String requestIp;

    @Column(name = "charge_details", columnDefinition = "TEXT")
    private String chargeDetails;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}