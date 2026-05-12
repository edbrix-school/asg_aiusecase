package com.asg.aiusecase.vector;

import java.util.Map;

public record InventoryVectorMatch(
        Long id,
        String productCode,
        String stockCode,
        String productName,
        String description,
        Map<String, Object> metadata,
        Map<String, Object> serialized,
        double cosineSimilarity
) {
}
