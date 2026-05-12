package com.asg.aiusecase.dto;

import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateInventoryRequest(
        @Size(max = 100) String productCode,
        @Size(max = 100) String stockCode,
        @Size(max = 255) String productName,
        String description,
        Map<String, Object> metadata
) {
}
