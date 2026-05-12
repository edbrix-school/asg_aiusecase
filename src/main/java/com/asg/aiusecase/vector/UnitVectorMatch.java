package com.asg.aiusecase.vector;

import java.util.Map;

public record UnitVectorMatch(
        Long id,
        Long inventoryId,
        String unitCode,
        String unitName,
        String description,
        Map<String, Object> metadata,
        Map<String, Object> serialized,
        double cosineSimilarity
) {
}
