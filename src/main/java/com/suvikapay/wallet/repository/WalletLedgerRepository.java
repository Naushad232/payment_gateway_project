package com.suvikapay.wallet.repository;

import com.suvikapay.wallet.entity.WalletLedger;
import com.suvikapay.wallet.entity.WalletLedgerId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletLedgerRepository extends JpaRepository<WalletLedger, WalletLedgerId> {
    List<WalletLedger> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<WalletLedger> findByPayinTxnId(String payinTxnId);
    List<WalletLedger> findByPayoutTxnId(String payoutTxnId);
}
