package com.asg.aiusecase.dto;

public record MatchedStockDto(
        Long stockPoid,
        String stockCode,
        String stockName
) {
}
