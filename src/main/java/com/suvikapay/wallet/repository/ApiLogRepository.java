package com.suvikapay.wallet.repository;

import com.suvikapay.wallet.entity.ApiLog;
import com.suvikapay.wallet.entity.ApiLogId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiLogRepository extends JpaRepository<ApiLog, ApiLogId> {
    List<ApiLog> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<ApiLog> findByTxnTypeAndTxnId(String txnType, String txnId);
}
