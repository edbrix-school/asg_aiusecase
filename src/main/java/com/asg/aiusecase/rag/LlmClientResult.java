package com.asg.aiusecase.rag;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public record LlmClientResult(
        JsonNode payload,
        Map<String, Object> metadata
) {
}
