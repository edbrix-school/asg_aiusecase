package com.asg.aiusecase.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryEvent(
        UUID eventId,
        InventoryEventType type,
        Long inventoryId,
        Long unitId,
        OffsetDateTime occurredAt
) {
    public InventoryEvent(InventoryEventType type, Long inventoryId, Long unitId) {
        this(UUID.randomUUID(), type, inventoryId, unitId, OffsetDateTime.now());
    }
}
