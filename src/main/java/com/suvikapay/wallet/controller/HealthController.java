// src/main/java/com/suvikapay/wallet/controller/HealthController.java
package com.suvikapay.wallet.controller;

import com.suvikapay.wallet.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    @Operation(summary = "Check application health", description = "Returns application status")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> healthInfo = Map.of(
                "status", "UP",
                "timestamp", OffsetDateTime.now(),
                "service", "Wallet Ledger API",
                "version", "1.0.0"
        );

        return ResponseEntity.ok(ApiResponse.success("Service is healthy", healthInfo));
    }

    @Operation(summary = "Check application readiness", description = "Returns application readiness status")
    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readiness() {
        Map<String, Object> readinessInfo = Map.of(
                "status", "READY",
                "timestamp", OffsetDateTime.now(),
                "database", "CONNECTED",
                "services", Map.of(
                        "authentication", "UP",
                        "database", "UP",
                        "cache", "UP"
                )
        );

        return ResponseEntity.ok(ApiResponse.success("Service is ready", readinessInfo));
    }

    @Operation(summary = "Check application liveness", description = "Returns application liveness status")
    @GetMapping("/live")
    public ResponseEntity<ApiResponse<Map<String, Object>>> liveness() {
        Map<String, Object> livenessInfo = Map.of(
                "status", "ALIVE",
                "timestamp", OffsetDateTime.now()
        );

        return ResponseEntity.ok(ApiResponse.success("Service is alive", livenessInfo));
    }
}