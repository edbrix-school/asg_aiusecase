package com.asg.aiusecase.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record UnitDto(
        Long id,
        Long inventoryId,
        String unitCode,
        String unitName,
        String description,
        Map<String, Object> metadata,
        Map<String, Object> serialized,
        Boolean validForInventory,
        Integer compatibilityPriority,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
