package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "wallet_ledger")
@IdClass(WalletLedgerId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletLedger {

    @Id
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Id
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ledger_id", nullable = false)
    private Long ledgerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private AppUser user;

    @Column(name = "entry_type", nullable = false)
    private String entryType;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "payin_txn_id")
    private String payinTxnId;

    @Column(name = "payout_txn_id")
    private String payoutTxnId;

    @Column(name = "remark")
    private String remark;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
