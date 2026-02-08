package com.suvikapay.wallet.repository;

import com.suvikapay.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByUser_UserId(Integer userId);
    Optional<Wallet> findByUser_EmailAddress(String email);
    boolean existsByUser_UserId(Integer userId);
}
