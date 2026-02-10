package com.suvikapay.wallet.service.impl;

import com.suvikapay.wallet.dto.request.CreateMerchantRequest;
import com.suvikapay.wallet.dto.response.MerchantResponse;
import com.suvikapay.wallet.entity.Merchant;
import com.suvikapay.wallet.entity.MerchantChargeSlab;
import com.suvikapay.wallet.exception.ResourceAlreadyExistsException;
import com.suvikapay.wallet.repository.MerchantChargeSlabRepository;
import com.suvikapay.wallet.repository.MerchantRepository;
import com.suvikapay.wallet.service.MerchantService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
@Service @RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {
    private final MerchantRepository merchantRepository;
    private final MerchantChargeSlabRepository merchantChargeSlabRepository;

    @Override
    @Transactional
    public MerchantResponse createMerchant(CreateMerchantRequest request) {
        // Enforce unique merchant name (db also has a unique constraint)
        if (merchantRepository.existsByMerchantName(request.getMerchantName())) {
            throw new ResourceAlreadyExistsException("Merchant", "merchantName", request.getMerchantName());
        }

        // Create and save Merchant
        Merchant merchant = Merchant.builder()
                .merchantName(request.getMerchantName())
                .build();

        Merchant savedMerchant;
        try {
            savedMerchant = merchantRepository.save(merchant);
        } catch (DataIntegrityViolationException ex) {
            // In case another thread created same merchant concurrently
            throw new ResourceAlreadyExistsException("Merchant", "merchantName", request.getMerchantName());
        }

        // Default serviceType if not provided in payload
        final String defaultServiceType = (request.getServiceType() == null || request.getServiceType().isBlank())
                ? "WALLET"
                : request.getServiceType();

        // Map and validate charge slabs
        List<MerchantChargeSlab> slabs = request.getCharges().stream().map(c -> {
            // Basic validation
            if (c.getStartAmt().compareTo(c.getEndAmt()) > 0) {
                throw new IllegalArgumentException("startAmt must be <= endAmt");
            }

            // Normalize values and strings
            BigDecimal start = c.getStartAmt().setScale(2, RoundingMode.HALF_UP);
            BigDecimal end = c.getEndAmt().setScale(2, RoundingMode.HALF_UP);
            BigDecimal charge = c.getCharge().setScale(4, RoundingMode.HALF_UP);
            BigDecimal gst = c.getGstPercent() != null ? c.getGstPercent().setScale(2, RoundingMode.HALF_UP) : null;
            String mode = c.getMode().trim().toUpperCase();        // e.g., PAYIN or PAYOUT
            String chargeType = c.getType().trim().toUpperCase();   // e.g., PERCENTAGE or FLAT

            return MerchantChargeSlab.builder()
                    .merchant(savedMerchant)
                    .serviceType(defaultServiceType)  // apply default or change as per your domain
                    .mode(mode)
                    .startAmount(start)
                    .endAmount(end)
                    .charge(charge)
                    .chargeType(chargeType)
                    .gstPercent(gst)
                    .build();
        }).toList();

        // Persist slabs
        List<MerchantChargeSlab> savedSlabs = merchantChargeSlabRepository.saveAll(slabs);

        // Build response
        return MerchantResponse.builder()
                .merchantId(savedMerchant.getMerchantId())
                .merchantName(savedMerchant.getMerchantName())
                .charges(savedSlabs.stream().map(s -> MerchantResponse.ChargeSlab.builder()
                                .slabId(s.getSlabId())
                                .serviceType(s.getServiceType())
                                .mode(s.getMode())
                                .startAmount(s.getStartAmount())
                                .endAmount(s.getEndAmount())
                                .charge(s.getCharge())
                                .chargeType(s.getChargeType())
                                .gstPercent(s.getGstPercent())
                                .build())
                        .toList())
                .build();
    }
}
