package com.asg.aiusecase.dto;

import com.asg.aiusecase.messaging.InventoryEventType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record PublishEmbeddingEventRequest(
        @NotNull InventoryEventType type,
        Long stockPoid,
        Long stockUnitPoid
) {
    @AssertTrue(message = "stockPoid is required for stock events and stockUnitPoid is required for stock unit events")
    public boolean hasRequiredRecordId() {
        if (type == null) {
            return true;
        }
        return switch (type) {
            case STOCK_CREATED, STOCK_UPDATED, STOCK_DELETED -> stockPoid != null;
            case STOCK_UNIT_CREATED, STOCK_UNIT_UPDATED, STOCK_UNIT_DELETED -> stockUnitPoid != null;
        };
    }
}
