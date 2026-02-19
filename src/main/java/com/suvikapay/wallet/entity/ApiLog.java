package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "api_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id", nullable = false, updatable = false)
    private Long logId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "txn_type", nullable = false)
    private String txnType;

    @Column(name = "txn_id", nullable = false)
    private String txnId;

    @Column(name = "service")
    private String service;

    @Column(name = "service_api")
    private String serviceApi;

    @Column(name = "request")
    private String request;

    @Column(name = "response")
    private String response;

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

