package com.asg.aiusecase.rag;

import com.asg.aiusecase.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiResponsesClient {

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public LlmClientResult resolve(String systemPrompt, String userPrompt, String modelOverride) {
        String model = modelOverride == null || modelOverride.isBlank()
                ? properties.getAi().getChatModel()
                : modelOverride;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", "stock_resolution",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "properties", Map.of(
                                "stockPoid", Map.of("type", List.of("number", "null")),
                                "stockUnitPoid", Map.of("type", List.of("number", "null")),
                                "confidence", Map.of("type", "number"),
                                "reason", Map.of("type", "string")
                        ),
                        "required", List.of("stockPoid", "stockUnitPoid", "confidence", "reason")
                )
        )));
        if (model.toLowerCase().startsWith("gpt-5")) {
            body.put("reasoning", Map.of("effort", properties.getAi().getReasoningEffort()));
        }

        JsonNode response = openAiRestClient.post()
                .uri("/v1/responses")
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String outputText = extractOutputText(response);
        JsonNode payload = parsePayload(outputText);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("model", model);
        if (response != null && response.has("usage")) {
            metadata.put("usage", objectMapper.convertValue(response.path("usage"), Map.class));
        }
        return new LlmClientResult(payload, metadata);
    }

    /**
     * Small/cheap model call to parse quantity, unit and batch information from free text.
     * Returns a JSON payload with properties: quantity (number|null), unit (string|null), batch (string|null).
     */
    public LlmClientResult parseQuantityBatch(String systemPrompt, String userPrompt, String modelOverride) {
        String model = modelOverride == null || modelOverride.isBlank()
                ? "gpt-4.1-nano"
                : modelOverride;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", "quantity_parsing",
                "strict", true,
                "schema", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "properties", Map.of(
                                "quantity", Map.of("type", List.of("number", "null")),
                                "unit", Map.of("type", List.of("string", "null")),
                                "batch", Map.of("type", List.of("string", "null"))
                        ),
                        "required", List.of("quantity", "unit", "batch")
                )
        )));
        // do not set reasoning effort for small model

        JsonNode response = openAiRestClient.post()
                .uri("/v1/responses")
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String outputText = extractOutputText(response);
        JsonNode payload = parsePayload(outputText);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("model", model);
        if (response != null && response.has("usage")) {
            metadata.put("usage", objectMapper.convertValue(response.path("usage"), Map.class));
        }
        return new LlmClientResult(payload, metadata);
    }

    public JsonNode parseQuantitiesBatch(String systemPrompt, String userPrompt, String modelOverride) {
        String model = modelOverride == null || modelOverride.isBlank()
                ? "gpt-4.1-nano"
                : modelOverride;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", "quantity_parsing_batch",
                "strict", true,
                "schema", Map.of(
                        "type", "array",
                        "items", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "id", Map.of("type", "string"),
                                        "quantity", Map.of("type", List.of("number", "null")),
                                        "unit", Map.of("type", List.of("string", "null")),
                                        "batch", Map.of("type", List.of("string", "null"))
                                ),
                                "required", List.of("id", "quantity", "unit", "batch")
                        )
                )
        )));

        JsonNode response = openAiRestClient.post()
                .uri("/v1/responses")
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String outputText = extractOutputText(response);
        return parsePayload(outputText);
    }

    public JsonNode resolveBatch(String systemPrompt, String userPrompt, String modelOverride) {
        String model = modelOverride == null || modelOverride.isBlank()
                ? properties.getAi().getChatModel()
                : modelOverride;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        body.put("text", Map.of("format", Map.of(
                "type", "json_schema",
                "name", "stock_resolution_batch",
                "strict", true,
                "schema", Map.of(
                        "type", "array",
                        "items", Map.of(
                                "type", "object",
                                "additionalProperties", false,
                                "properties", Map.of(
                                        "id", Map.of("type", "string"),
                                        "stockPoid", Map.of("type", List.of("number", "null")),
                                        "stockUnitPoid", Map.of("type", List.of("number", "null")),
                                        "confidence", Map.of("type", "number"),
                                        "reason", Map.of("type", "string")
                                ),
                                "required", List.of("id", "stockPoid", "stockUnitPoid", "confidence", "reason")
                        )
                )
        )));
        if (model.toLowerCase().startsWith("gpt-5")) {
            body.put("reasoning", Map.of("effort", properties.getAi().getReasoningEffort()));
        }

        JsonNode response = openAiRestClient.post()
                .uri("/v1/responses")
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String outputText = extractOutputText(response);
        return parsePayload(outputText);
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI response was empty");
        }
        if (response.hasNonNull("output_text")) {
            return response.path("output_text").asText();
        }
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if (content.hasNonNull("text")) {
                    return content.path("text").asText();
                }
            }
        }
        throw new IllegalStateException("OpenAI response did not include output text");
    }

    private JsonNode parsePayload(String outputText) {
        try {
            return objectMapper.readTree(outputText);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI response was not valid JSON", e);
        }
    }
}
