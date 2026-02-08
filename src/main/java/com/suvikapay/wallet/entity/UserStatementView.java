package com.suvikapay.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLInetJdbcType;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.time.OffsetDateTime;

@Entity
@Immutable
@Table(name = "v_user_statement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserStatementView {

    @Id
    @Column(name = "txn_id")
    private String txnId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "txn_type")
    private String txnType;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "status")
    private String status;

    @Column(name = "txn_amount", precision = 18, scale = 2)
    private BigDecimal txnAmount;

    @Column(name = "charge", precision = 18, scale = 2)
    private BigDecimal charge;

    @Column(name = "gst", precision = 18, scale = 2)
    private BigDecimal gst;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "utr")
    private String utr;

    @JdbcType(PostgreSQLInetJdbcType.class)
    @Column(name = "ip_address")
    private InetAddress ipAddress;


    @Column(name = "bene_name")
    private String beneName;

    @Column(name = "bene_account")
    private String beneAccount;

    @Column(name = "bene_ifsc")
    private String beneIfsc;

    @Column(name = "bene_bank")
    private String beneBank;

    @Column(name = "mode")
    private String mode;
}
