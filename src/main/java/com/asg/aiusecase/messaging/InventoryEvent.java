package com.asg.aiusecase.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryEvent(
        UUID eventId,
        InventoryEventType type,
        Long stockPoid,
        Long stockUnitPoid,
        OffsetDateTime occurredAt
) {
    public InventoryEvent(InventoryEventType type, Long stockPoid, Long stockUnitPoid) {
        this(UUID.randomUUID(), type, stockPoid, stockUnitPoid, OffsetDateTime.now());
    }
}
