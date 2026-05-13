package com.asg.aiusecase.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record UnitDto(
        Long stockUnitPoid,
        String stockUnitCode,
        String stockUnitName,
        Map<String, Object> metadata,
        Map<String, Object> serialized,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
