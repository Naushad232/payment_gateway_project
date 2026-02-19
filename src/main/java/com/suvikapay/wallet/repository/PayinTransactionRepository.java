package com.suvikapay.wallet.repository;

import com.suvikapay.wallet.entity.PayinTransaction;
import com.suvikapay.wallet.entity.PayinTransactionId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PayinTransactionRepository extends JpaRepository<PayinTransaction, PayinTransactionId> {
    Optional<PayinTransaction> findByOrderId(String orderId);

    Optional<PayinTransaction> findByTxnId(String txnId);

    List<PayinTransaction> findByUserId(Integer userId);

    List<PayinTransaction> findByUserIdAndStatus(Integer userId, String status);

    List<PayinTransaction> findByUserIdAndStatusAndCreatedAtBetween(
            Integer userId, String status, OffsetDateTime start, OffsetDateTime end);

//    @Query("SELECT p FROM PayinTransaction p WHERE p.isChargeBack = true ORDER BY p.chargeBackDate DESC")
//    Page<PayinTransaction> findChargebacks(Pageable pageable);

//    @Query("SELECT p FROM PayinTransaction p WHERE p.userId = :userId AND p.isChargeBack = true")
//    List<PayinTransaction> findChargebacksByUserId(@Param("userId") Integer userId);
}
