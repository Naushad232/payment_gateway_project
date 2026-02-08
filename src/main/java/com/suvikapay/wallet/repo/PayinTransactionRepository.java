package com.suvikapay.wallet.repo;

import com.suvikapay.wallet.entity.PayinTransaction;
import com.suvikapay.wallet.entity.PayinTransactionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayinTransactionRepository extends JpaRepository<PayinTransaction, PayinTransactionId> {
    List<PayinTransaction> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<PayinTransaction> findByStatusOrderByCreatedAtDesc(String status);
    List<PayinTransaction> findByOrderId(String orderId);
}
