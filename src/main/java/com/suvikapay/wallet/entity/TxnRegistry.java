package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "txn_registry")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TxnRegistry {

    @Id
    @Column(name = "txn_id")
    private String txnId;

    @Column(name = "txn_type", nullable = false)
    private String txnType;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
