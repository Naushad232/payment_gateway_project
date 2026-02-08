package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLInetJdbcType;

import java.net.InetAddress;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_ips",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_ips_user_ip", columnNames = {"user_id", "ip_address"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @JdbcType(PostgreSQLInetJdbcType.class)
    @Column(name = "ip_address", nullable = false)
    private InetAddress ipAddress;


    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
