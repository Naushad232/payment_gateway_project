package com.suvikapay.wallet.repo;

import com.suvikapay.wallet.entity.PayoutTransaction;
import com.suvikapay.wallet.entity.PayoutTransactionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayoutTransactionRepository extends JpaRepository<PayoutTransaction, PayoutTransactionId> {
    List<PayoutTransaction> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<PayoutTransaction> findByStatusOrderByCreatedAtDesc(String status);
    List<PayoutTransaction> findByOrderId(String orderId);
}
