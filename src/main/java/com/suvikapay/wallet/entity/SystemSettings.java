package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemSettings {

    @Id
    @Column(name = "id")
    private Short id = 1;

    @Column(name = "maintainence", nullable = false)
    private Boolean maintainence = false;

    @Column(name = "payin_service", nullable = false)
    private Boolean payinService = true;

    @Column(name = "payout_service", nullable = false)
    private Boolean payoutService = true;
}
