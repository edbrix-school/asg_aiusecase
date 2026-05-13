package com.asg.aiusecase.service;

public record LlmSelectionOverride(
        Long stockPoid,
        Long stockUnitPoid,
        Double confidence,
        String reason
) {
}
