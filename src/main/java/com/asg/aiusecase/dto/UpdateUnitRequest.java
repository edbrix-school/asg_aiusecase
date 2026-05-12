package com.asg.aiusecase.dto;

import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateUnitRequest(
        @Size(max = 100) String unitCode,
        @Size(max = 255) String unitName,
        String description,
        Map<String, Object> metadata,
        Boolean validForInventory,
        Integer compatibilityPriority
) {
}
