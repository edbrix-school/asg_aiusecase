package com.asg.aiusecase.rag;

public record LlmResolutionResult(
        Long stockPoid,
        Long stockUnitPoid,
        double confidence,
        String reason
) {
}
