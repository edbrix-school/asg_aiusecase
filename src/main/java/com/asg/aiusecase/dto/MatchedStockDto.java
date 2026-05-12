package com.asg.aiusecase.dto;

public record MatchedStockDto(
        Long inventoryId,
        String stockCode,
        String productCode,
        String productName
) {
}
