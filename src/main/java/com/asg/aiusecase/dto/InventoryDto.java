package com.asg.aiusecase.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record InventoryDto(
        Long id,
        String productCode,
        String stockCode,
        String productName,
        String description,
        Map<String, Object> metadata,
        Map<String, Object> serialized,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
