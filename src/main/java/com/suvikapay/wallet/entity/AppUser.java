package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email_address"),
                @UniqueConstraint(name = "uk_users_username", columnNames = "user_name")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "user_type")
    private String userType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paying_merchant_id")
    private Merchant payingMerchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_merchant_id")
    private Merchant payoutMerchant;

    @Column(name = "paying_api_status")
    private String payingApiStatus;

    @Column(name = "payout_api_status")
    private String payoutApiStatus;

    @Column(name = "user_token")
    private String userToken;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_login")
    private OffsetDateTime lastLogin;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
        if (isActive == null) isActive = true;
    }
}
