package com.asg.aiusecase.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank String query,
        @Min(1) @Max(50) Integer topK,
        Double minSimilarity,
        String embeddingModel,
        String chatModel,
        String fallbackModel,
        Boolean allowLlm,
        Boolean includeCandidates
) {
}
