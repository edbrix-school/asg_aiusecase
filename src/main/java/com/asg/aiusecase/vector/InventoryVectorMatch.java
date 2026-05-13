package com.asg.aiusecase.vector;

import java.util.Map;

public record InventoryVectorMatch(
        Long id,
        String stockCode,
        String stockName,
        String stockDescription,
        Map<String, Object> metadata,
        Map<String, Object> serialized,
        double cosineSimilarity
) {
}
