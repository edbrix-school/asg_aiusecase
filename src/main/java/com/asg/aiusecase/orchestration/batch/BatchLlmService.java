package com.asg.aiusecase.orchestration.batch;

import com.asg.aiusecase.businesslogic.ResolvedCandidate;
import com.asg.aiusecase.config.AppProperties;
import com.asg.aiusecase.ingestion.InventoryLine;
import com.asg.aiusecase.rag.OpenAiResponsesClient;
import com.asg.aiusecase.rag.RagPromptBuilder;
import com.asg.aiusecase.service.LlmSelectionOverride;
import com.asg.aiusecase.service.ParsedQuantity;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchLlmService {

    private static final Pattern QTY_UNIT_AT_END = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([A-Za-z]+)?\\s*$");

    private final OpenAiResponsesClient openAiResponsesClient;
    private final RagPromptBuilder promptBuilder;
    private final AppProperties properties;

    public Map<String, ParsedQuantity> parseQuantities(List<InventoryLine> lines) {
        Map<String, ParsedQuantity> results = new HashMap<>();
        if (lines == null || lines.isEmpty()) {
            return results;
        }

        List<InventoryLine> llmLines = lines.stream()
                .filter(line -> Boolean.TRUE.equals(line.options().llmParseQuantity()))
                .toList();

        // Regex fallback for non-LLM lines
        lines.stream()
                .filter(line -> !Boolean.TRUE.equals(line.options().llmParseQuantity()))
                .forEach(line -> results.put(line.id(), regexParse(line.normalizedText())));

        if (llmLines.isEmpty()) {
            return results;
        }

        int batchSize = properties.getBatch().getQuantityBatchSize();
        for (int i = 0; i < llmLines.size(); i += batchSize) {
            List<InventoryLine> batch = llmLines.subList(i, Math.min(i + batchSize, llmLines.size()));
            Map<String, ParsedQuantity> batchResult = tryParseQuantityBatch(batch);
            results.putAll(batchResult);
            // fill missing with regex fallback
            batch.stream()
                    .filter(line -> !results.containsKey(line.id()))
                    .forEach(line -> results.put(line.id(), regexParse(line.normalizedText())));
        }
        return results;
    }

    public Map<String, LlmSelectionOverride> resolveAmbiguities(List<BatchAmbiguityItem> items) {
        Map<String, LlmSelectionOverride> results = new HashMap<>();
        if (items == null || items.isEmpty()) {
            return results;
        }

        Map<String, List<BatchAmbiguityItem>> byModel = items.stream()
                .collect(Collectors.groupingBy(item -> defaultChatModel(item.chatModel())));
        for (Map.Entry<String, List<BatchAmbiguityItem>> entry : byModel.entrySet()) {
            String model = entry.getKey();
            List<BatchAmbiguityItem> group = entry.getValue();
            int batchSize = properties.getBatch().getAmbiguityBatchSize();
            for (int i = 0; i < group.size(); i += batchSize) {
                List<BatchAmbiguityItem> batch = group.subList(i, Math.min(i + batchSize, group.size()));
                Map<String, LlmSelectionOverride> batchResult = tryResolveBatch(batch, model);
                results.putAll(batchResult);
                // fallback to secondary model if present and primary didn't return
                List<BatchAmbiguityItem> missing = batch.stream()
                        .filter(item -> !results.containsKey(item.lineId()))
                        .filter(item -> item.fallbackModel() != null && !item.fallbackModel().isBlank())
                        .filter(item -> !item.fallbackModel().equalsIgnoreCase(model))
                        .toList();
                if (!missing.isEmpty()) {
                    String fallback = missing.get(0).fallbackModel();
                    Map<String, LlmSelectionOverride> fallbackResult = tryResolveBatch(missing, fallback);
                    results.putAll(fallbackResult);
                }
            }
        }
        return results;
    }

    private Map<String, ParsedQuantity> tryParseQuantityBatch(List<InventoryLine> batch) {
        String systemPrompt = "Extract quantity, unit (normalize to short form), and batch identifier for each line. Respond with JSON array of {id:string,quantity:number|null,unit:string|null,batch:string|null}.";
        StringBuilder userPrompt = new StringBuilder();
        for (InventoryLine line : batch) {
            userPrompt.append("id:").append(line.id()).append(" | ").append(line.normalizedText()).append("\n");
        }

        Map<String, ParsedQuantity> results = new HashMap<>();
        int attempts = properties.getBatch().getLlmRetryAttempts();
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                JsonNode payload = openAiResponsesClient.parseQuantitiesBatch(systemPrompt, userPrompt.toString(), defaultParseModel(batch));
                if (payload != null && payload.isArray()) {
                    for (JsonNode item : payload) {
                        String id = item.path("id").asText(null);
                        if (id == null) {
                            continue;
                        }
                        Double quantity = item.path("quantity").isNumber() ? item.path("quantity").asDouble() : null;
                        String unit = item.path("unit").isTextual() ? item.path("unit").asText().toUpperCase() : null;
                        String batchId = item.path("batch").isTextual() ? item.path("batch").asText() : null;
                        results.put(id, new ParsedQuantity(quantity, unit, batchId));
                    }
                }
                return results;
            } catch (Exception e) {
                log.debug("Batch quantity parse attempt {} failed: {}", attempt, e.getMessage());
            }
        }
        return results;
    }

    private Map<String, LlmSelectionOverride> tryResolveBatch(List<BatchAmbiguityItem> batch, String model) {
        String systemPrompt = "You are a stock ambiguity resolver. For each line, choose from the provided candidate stockPoid/stockUnitPoid pairs. Respond with JSON array of {id:string,stockPoid:number|null,stockUnitPoid:number|null,confidence:number,reason:string}.";
        StringBuilder userPrompt = new StringBuilder();
        for (BatchAmbiguityItem item : batch) {
            userPrompt.append("Line ").append(item.lineId()).append(":\n");
            userPrompt.append(promptBuilder.userPrompt(item.query(), item.candidates())).append("\n");
        }

        Map<String, LlmSelectionOverride> results = new HashMap<>();
        int attempts = properties.getBatch().getLlmRetryAttempts();
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                JsonNode payload = openAiResponsesClient.resolveBatch(systemPrompt, userPrompt.toString(), model);
                if (payload != null && payload.isArray()) {
                    for (JsonNode item : payload) {
                        String id = item.path("id").asText(null);
                        if (id == null) {
                            continue;
                        }
                        Long stockPoid = item.path("stockPoid").isNumber() ? item.path("stockPoid").asLong() : null;
                        Long stockUnitPoid = item.path("stockUnitPoid").isNumber() ? item.path("stockUnitPoid").asLong() : null;
                        Double confidence = item.path("confidence").isNumber() ? item.path("confidence").asDouble() : null;
                        String reason = item.path("reason").isTextual() ? item.path("reason").asText() : null;
                        results.put(id, new LlmSelectionOverride(stockPoid, stockUnitPoid, confidence, reason));
                    }
                }
                return results;
            } catch (Exception e) {
                log.debug("Batch ambiguity resolve attempt {} failed: {}", attempt, e.getMessage());
            }
        }
        return results;
    }

    private ParsedQuantity regexParse(String text) {
        if (text == null) {
            return new ParsedQuantity(null, null, null);
        }
        Double quantity = null;
        String unit = null;
        Matcher matcher = QTY_UNIT_AT_END.matcher(text.trim());
        if (matcher.find()) {
            String qStr = matcher.group(1);
            String uStr = matcher.group(2);
            if (qStr != null && !qStr.isBlank()) {
                try {
                    quantity = Double.parseDouble(qStr);
                } catch (Exception ignored) {
                    // ignore parse errors
                }
            }
            if (uStr != null && !uStr.isBlank()) {
                unit = uStr.trim().toUpperCase();
            }
        }
        return new ParsedQuantity(quantity, unit, null);
    }

    private String defaultParseModel(List<InventoryLine> batch) {
        for (InventoryLine line : batch) {
            if (line.options().llmParseModel() != null && !line.options().llmParseModel().isBlank()) {
                return line.options().llmParseModel();
            }
        }
        return "gpt-4.1-nano";
    }

    private String defaultChatModel(String model) {
        if (model != null && !model.isBlank()) {
            return model;
        }
        return properties.getAi().getChatModel();
    }
}
