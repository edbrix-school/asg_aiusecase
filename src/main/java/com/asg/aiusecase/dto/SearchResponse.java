package com.asg.aiusecase.dto;

import java.util.List;
import java.util.Map;

public record SearchResponse(
        InventoryDto matchedInventory,
        MatchedStockDto matchedStock,
        UnitDto matchedUnit,
        double confidenceScore,
        double cosineSimilarity,
        ReasoningSource reasoningSource,
        Map<String, Object> reasoningMetadata,
        AmbiguityStatus ambiguityStatus,
        List<CandidateDto> candidates
) {
}
