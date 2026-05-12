package com.asg.aiusecase.dto;

public record CandidateDto(
        Long inventoryId,
        Long unitId,
        String stockCode,
        String productName,
        String unitCode,
        String unitName,
        double confidence,
        double inventorySimilarity,
        double unitSimilarity,
        boolean compatible,
        String reason
) {
}
