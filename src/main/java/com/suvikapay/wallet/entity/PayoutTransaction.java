package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLInetJdbcType;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payout_transactions")
@IdClass(PayoutTransactionId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayoutTransaction {

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

    @Column(name = "mode")
    private String mode;

    @Column(name = "bene_name")
    private String beneName;

    @Column(name = "bene_account")
    private String beneAccount;

    @Column(name = "bene_ifsc")
    private String beneIfsc;

    @Column(name = "bene_bank")
    private String beneBank;

    @Column(name = "utr")
    private String utr;

    @Column(name = "remark")
    private String remark;

    @JdbcType(PostgreSQLInetJdbcType.class)
    @Column(name = "ip_address")
    private InetAddress ipAddress;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "status_pending_date")
    private OffsetDateTime statusPendingDate;

    @Column(name = "status_success_date")
    private OffsetDateTime statusSuccessDate;

    @Column(name = "status_failed_date")
    private OffsetDateTime statusFailedDate;

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
