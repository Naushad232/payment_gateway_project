package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "merchants",
        uniqueConstraints = @UniqueConstraint(name = "uk_merchants_name", columnNames = "merchant_name"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "merchant_name", nullable = false, unique = true)
    private String merchantName;
}
