package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLInetJdbcType;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payin_transactions")
@IdClass(PayinTransactionId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayinTransaction {

    @Id
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Id
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Id
    @Column(name = "txn_id", nullable = false)
    private String txnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "charge", precision = 18, scale = 2)
    private BigDecimal charge;

    @Column(name = "gst", precision = 18, scale = 2)
    private BigDecimal gst;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "admin_charge", precision = 18, scale = 2)
    private BigDecimal adminCharge;

    @Column(name = "admin_tax", precision = 18, scale = 2)
    private BigDecimal adminTax;

    @Column(name = "agent_charge", precision = 18, scale = 2)
    private BigDecimal agentCharge;

    @Column(name = "agent_tax", precision = 18, scale = 2)
    private BigDecimal agentTax;

    @Column(name = "merchant_charge", precision = 18, scale = 2)
    private BigDecimal merchantCharge;

    @Column(name = "merchant_gst", precision = 18, scale = 2)
    private BigDecimal merchantGst;

    @Column(name = "payer_name")
    private String payerName;

    @Column(name = "payer_upi")
    private String payerUpi;

    @Column(name = "utr")
    private String utr;

    @Column(name = "t_id")
    private String tId;

    @Column(name = "api")
    private String api;

    @JdbcType(PostgreSQLInetJdbcType.class)
    @Column(name = "ip_address")
    private InetAddress ipAddress;

    @Column(name = "status", nullable = false)
    private String status;

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
