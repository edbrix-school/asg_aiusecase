package com.asg.aiusecase.rag;

public record LlmResolutionResult(
        Long inventoryId,
        Long unitId,
        double confidence,
        String reason
) {
}
