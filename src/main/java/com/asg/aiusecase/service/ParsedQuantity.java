package com.asg.aiusecase.service;

public record ParsedQuantity(
        Double quantity,
        String unit,
        String batch
) {
}
