package com.suvikapay.wallet.service;

import com.suvikapay.wallet.dto.request.CreateMerchantRequest;
import com.suvikapay.wallet.dto.response.MerchantResponse;
public interface MerchantService { MerchantResponse createMerchant(CreateMerchantRequest request); }
