// src/main/java/com/suvikapay/wallet/dto/PaginationRequest.java
package com.suvikapay.wallet.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequest {

    @Min(value = 0, message = "Page must be greater than or equal to 0")
    private int page = 0;

    @Min(value = 1, message = "Size must be greater than or equal to 1")
    private int size = 20;

    private String sortBy = "createdAt";
    private String sortDirection = "DESC";

    public int getOffset() {
        return page * size;
    }
}