package com.asg.aiusecase.ingestion.parser;

import com.asg.aiusecase.ingestion.InventoryLine;
import com.asg.aiusecase.ingestion.SearchOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QueryPayloadParser {

    private final ObjectMapper objectMapper;

    public ParsedQuery parse(String payload) {
        if (payload == null || payload.isBlank()) {
            return new ParsedQuery(List.of(), SearchOptions.empty());
        }

        String trimmed = payload.trim();
        if (looksLikeJson(trimmed)) {
            try {
                JsonNode root = objectMapper.readTree(trimmed);
                if (root.isArray()) {
                    return new ParsedQuery(parseArray(root), SearchOptions.empty());
                }
                if (root.isObject()) {
                    return parseObject(root);
                }
            } catch (Exception ignored) {
                // fall back to text parsing
            }
        }

        return new ParsedQuery(parseMultilineText(trimmed, "query"), SearchOptions.empty());
    }

    public SearchOptions parseOptionsPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return SearchOptions.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(payload.trim());
            if (root != null && root.isObject()) {
                return parseOptions(root);
            }
        } catch (Exception ignored) {
            // ignore invalid meta payload
        }
        return SearchOptions.empty();
    }

    private ParsedQuery parseObject(JsonNode root) {
        SearchOptions defaults = parseOptions(root.path("options"));
        List<InventoryLine> lines = new ArrayList<>();
        JsonNode linesNode = root.path("lines");
        if (linesNode.isArray()) {
            lines.addAll(parseLinesArray(linesNode));
        } else if (root.hasNonNull("query")) {
            lines.addAll(parseMultilineText(root.path("query").asText(""), "query"));
        }
        return new ParsedQuery(lines, defaults);
    }

    private List<InventoryLine> parseArray(JsonNode root) {
        List<InventoryLine> lines = new ArrayList<>();
        if (!root.isArray()) {
            return lines;
        }
        boolean allText = true;
        for (JsonNode node : root) {
            if (!node.isTextual()) {
                allText = false;
                break;
            }
        }
        if (allText) {
            for (JsonNode node : root) {
                String raw = node.asText("");
                addLine(lines, raw, "query");
            }
            return lines;
        }
        return parseLinesArray(root);
    }

    private List<InventoryLine> parseLinesArray(JsonNode root) {
        List<InventoryLine> lines = new ArrayList<>();
        for (JsonNode node : root) {
            if (node.isTextual()) {
                addLine(lines, node.asText(""), "query");
                continue;
            }
            if (!node.isObject()) {
                continue;
            }
            String raw = node.path("text").asText("");
            SearchOptions options = parseOptions(node.path("options"));
            addLine(lines, raw, "query", options);
        }
        return lines;
    }

    private List<InventoryLine> parseMultilineText(String text, String source) {
        List<InventoryLine> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        for (String raw : text.split("\\r?\\n")) {
            addLine(lines, raw, source);
        }
        return lines;
    }

    private void addLine(List<InventoryLine> lines, String raw, String source) {
        addLine(lines, raw, source, SearchOptions.empty());
    }

    private void addLine(List<InventoryLine> lines, String raw, String source, SearchOptions options) {
        if (raw == null) {
            return;
        }
        String normalized = raw.trim();
        if (normalized.isBlank()) {
            return;
        }
        lines.add(new InventoryLine(UUID.randomUUID().toString(), raw, normalized, source, options));
    }

    private SearchOptions parseOptions(JsonNode optionsNode) {
        if (optionsNode == null || !optionsNode.isObject()) {
            return SearchOptions.empty();
        }
        return new SearchOptions(
                optionsNode.path("topK").isNumber() ? optionsNode.path("topK").asInt() : null,
                optionsNode.path("minSimilarity").isNumber() ? optionsNode.path("minSimilarity").asDouble() : null,
                textOrNull(optionsNode.path("embeddingModel")),
                textOrNull(optionsNode.path("chatModel")),
                textOrNull(optionsNode.path("fallbackModel")),
                optionsNode.path("allowLlm").isBoolean() ? optionsNode.path("allowLlm").asBoolean() : null,
                optionsNode.path("includeCandidates").isBoolean() ? optionsNode.path("includeCandidates").asBoolean() : null,
                optionsNode.path("llmParseQuantity").isBoolean() ? optionsNode.path("llmParseQuantity").asBoolean() : null,
                textOrNull(optionsNode.path("llmParseModel"))
        );
    }

    private String textOrNull(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private boolean looksLikeJson(String payload) {
        return payload.startsWith("{") || payload.startsWith("[");
    }
}
