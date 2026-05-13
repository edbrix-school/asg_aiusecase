package com.asg.aiusecase.vector;

import java.util.Map;

public record UnitVectorMatch(
        Long id,
        String stockUnitCode,
        String stockUnitName,
        Map<String, Object> metadata,
        Map<String, Object> serialized,
        double cosineSimilarity
) {
}
