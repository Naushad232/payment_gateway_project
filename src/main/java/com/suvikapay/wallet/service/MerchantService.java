package com.suvikapay.wallet.service;

import com.suvikapay.wallet.dto.request.CreateMerchantRequest;
import com.suvikapay.wallet.dto.response.MerchantResponse;

import java.util.List;

public interface MerchantService {
    MerchantResponse createMerchant(CreateMerchantRequest request);

    List<MerchantResponse> getAllMerchants();

    MerchantResponse getMerchantById(Long merchantId);

    MerchantResponse updateMerchant(Long merchantId, CreateMerchantRequest request);
}
