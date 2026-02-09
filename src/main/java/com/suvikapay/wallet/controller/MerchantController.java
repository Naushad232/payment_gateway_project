package com.suvikapay.wallet.controller;

import com.suvikapay.wallet.dto.request.CreateMerchantRequest;
import com.suvikapay.wallet.dto.response.MerchantResponse;
import com.suvikapay.wallet.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    // POST /api/v1/merchants
    @PostMapping
    public ResponseEntity<MerchantResponse> create(@Valid @RequestBody CreateMerchantRequest request) {
        MerchantResponse created = merchantService.createMerchant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
