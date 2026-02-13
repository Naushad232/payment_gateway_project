package com.suvikapay.wallet.controller;

import com.suvikapay.wallet.dto.request.CreateMerchantRequest;
import com.suvikapay.wallet.dto.response.ApiResponse;
import com.suvikapay.wallet.dto.response.MerchantResponse;
import com.suvikapay.wallet.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;


    // CREATE
    @PostMapping
    public ResponseEntity<ApiResponse<MerchantResponse>> create(
            @Valid @RequestBody CreateMerchantRequest request) {

        MerchantResponse created = merchantService.createMerchant(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Merchant created successfully", created));
    }

    // LIST
    @GetMapping
    public ResponseEntity<ApiResponse<List<MerchantResponse>>> list() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Merchants fetched successfully",
                        merchantService.getAllMerchants()
                )
        );
    }

    // FIND BY ID
    @GetMapping("/{merchantId}")
    public ResponseEntity<ApiResponse<MerchantResponse>> getById(
            @PathVariable Long merchantId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Merchant fetched successfully",
                        merchantService.getMerchantById(merchantId)
                )
        );
    }

    // UPDATE
    @PutMapping("/{merchantId}")
    public ResponseEntity<ApiResponse<MerchantResponse>> update(
            @PathVariable Long merchantId,
            @Valid @RequestBody CreateMerchantRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Merchant updated successfully",
                        merchantService.updateMerchant(merchantId, request)
                )
        );
    }
}
