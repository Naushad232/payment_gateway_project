package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "merchant_charge_slabs",
        indexes = @Index(name = "idx_merchant_charge_slabs_lookup", columnList = "merchant_id,service_type,mode"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MerchantChargeSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slab_id")
    private Long slabId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "mode", nullable = false)
    private String mode;

    @Column(name = "start_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal startAmount;

    @Column(name = "end_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal endAmount;

    @Column(name = "charge", nullable = false, precision = 18, scale = 4)
    private BigDecimal charge;

    @Column(name = "charge_type", nullable = false)
    private String chargeType;

    @Column(name = "gst_percent", precision = 5, scale = 2)
    private BigDecimal gstPercent;
}
