package com.asg.aiusecase.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record InventoryDto(
        Long stockPoid,
        String stockCode,
        String stockName,
        String stockDescription,
        Map<String, Object> metadata,
        Map<String, Object> serialized,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
