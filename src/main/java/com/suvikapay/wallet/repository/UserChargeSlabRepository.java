package com.suvikapay.wallet.repository;

import com.suvikapay.wallet.entity.UserChargeSlab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface UserChargeSlabRepository extends JpaRepository<UserChargeSlab, Long> {
    List<UserChargeSlab> findByUserUserId(Integer userId);

    List<UserChargeSlab> findByUserUserIdAndStartAmountLessThanEqualAndEndAmountGreaterThanEqual(
            Integer userId, BigDecimal amount1, BigDecimal amount2
    );
}
