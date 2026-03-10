package com.suvikapay.wallet.repository;

import com.suvikapay.wallet.entity.PayoutTransaction;
import com.suvikapay.wallet.entity.PayoutTransactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PayoutTransactionRepository extends JpaRepository<PayoutTransaction, PayoutTransactionId> {




    Optional<PayoutTransaction> findByOrderId(String orderId);

    List<PayoutTransaction> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<PayoutTransaction> findByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT p FROM PayoutTransaction p WHERE p.userId = :userId AND p.orderId = :orderId")
    Optional<PayoutTransaction> findByUserIdAndOrderId(@Param("userId") Integer userId,
                                                       @Param("orderId") String orderId);

    @Query("SELECT SUM(p.totalAmount) FROM PayoutTransaction p WHERE p.userId = :userId AND p.status = 'SUCCESS' AND p.createdAt > :since")
    BigDecimal getTotalPayoutSince(@Param("userId") Integer userId, @Param("since") OffsetDateTime since);
}
