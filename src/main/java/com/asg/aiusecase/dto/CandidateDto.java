package com.asg.aiusecase.dto;

public record CandidateDto(
        Long stockPoid,
        Long stockUnitPoid,
        String stockCode,
        String stockName,
        String stockUnitCode,
        String stockUnitName,
        double confidence,
        double stockSimilarity,
        double unitSimilarity,
        boolean compatible,
        String reason
) {
}
