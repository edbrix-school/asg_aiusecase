package com.asg.aiusecase.ingestion;

public record InventoryLine(
        String id,
        String rawText,
        String normalizedText,
        String source,
        SearchOptions options
) {
}
