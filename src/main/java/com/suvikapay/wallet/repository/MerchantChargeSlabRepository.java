package com.suvikapay.wallet.repository;

import com.suvikapay.wallet.entity.MerchantChargeSlab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface MerchantChargeSlabRepository extends JpaRepository<MerchantChargeSlab, Long> {

    List<MerchantChargeSlab> findByMerchantMerchantId(Long merchantId);

    List<MerchantChargeSlab> findByMerchantMerchantIdAndServiceTypeAndMode(Long merchantId, String serviceType, String mode);

    List<MerchantChargeSlab> findByMerchantMerchantIdAndServiceTypeAndModeAndStartAmountLessThanEqualAndEndAmountGreaterThanEqual(
            Long merchantId, String serviceType, String mode, BigDecimal amount1, BigDecimal amount2
    );
}
