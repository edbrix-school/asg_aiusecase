package com.asg.aiusecase.dto;

import java.util.List;
import java.util.Map;

public record SearchResponse(
        MatchedStockDto matchedStock,
        MatchedUnitDto matchedStockUnit,
        double confidenceScore,
        double cosineSimilarity,
        ReasoningSource reasoningSource,
        Map<String, Object> reasoningMetadata,
        AmbiguityStatus ambiguityStatus,
        List<CandidateDto> candidates,
        Double parsedQuantity,
        String parsedUnit,
        String parsedBatch
) {
}
