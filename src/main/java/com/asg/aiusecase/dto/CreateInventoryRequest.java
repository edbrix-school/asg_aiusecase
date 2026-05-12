package com.asg.aiusecase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateInventoryRequest(
        @NotBlank @Size(max = 100) String productCode,
        @NotBlank @Size(max = 100) String stockCode,
        @NotBlank @Size(max = 255) String productName,
        String description,
        Map<String, Object> metadata
) {
}
