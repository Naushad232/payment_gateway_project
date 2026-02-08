package com.suvikapay.wallet.repo;

import com.suvikapay.wallet.entity.TxnRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TxnRegistryRepository extends JpaRepository<TxnRegistry, String> {
    List<TxnRegistry> findByTxnTypeOrderByCreatedAtDesc(String txnType);
}
