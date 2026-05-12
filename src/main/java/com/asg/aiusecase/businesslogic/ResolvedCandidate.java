package com.asg.aiusecase.businesslogic;

import com.asg.aiusecase.entity.InventoryEntity;
import com.asg.aiusecase.entity.UnitEntity;

public record ResolvedCandidate(
        InventoryEntity inventory,
        UnitEntity unit,
        double inventorySimilarity,
        double unitSimilarity,
        boolean stockExact,
        boolean synonymMatched,
        boolean compatible,
        double confidence,
        String reason
) {
}
