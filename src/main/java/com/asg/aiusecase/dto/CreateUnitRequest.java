package com.asg.aiusecase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateUnitRequest(
        @NotNull Long inventoryId,
        @NotBlank @Size(max = 100) String unitCode,
        @NotBlank @Size(max = 255) String unitName,
        String description,
        Map<String, Object> metadata,
        Boolean validForInventory,
        Integer compatibilityPriority
) {
}
