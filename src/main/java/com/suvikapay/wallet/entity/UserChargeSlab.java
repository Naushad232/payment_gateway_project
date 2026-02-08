package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "user_charge_slabs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserChargeSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slab_id")
    private Long slabId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "start_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal startAmount;

    @Column(name = "end_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal endAmount;

    @Column(name = "payin_charge", precision = 18, scale = 4)
    private BigDecimal payinCharge;

    @Column(name = "payin_charge_type")
    private String payinChargeType;

    @Column(name = "payout_charge", precision = 18, scale = 4)
    private BigDecimal payoutCharge;

    @Column(name = "payout_charge_type")
    private String payoutChargeType;

    @Column(name = "agent_payin_charge", precision = 18, scale = 4)
    private BigDecimal agentPayinCharge;

    @Column(name = "agent_payout_charge", precision = 18, scale = 4)
    private BigDecimal agentPayoutCharge;
}
