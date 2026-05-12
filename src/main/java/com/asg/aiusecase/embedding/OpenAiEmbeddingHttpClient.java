package com.asg.aiusecase.embedding;

import com.asg.aiusecase.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingHttpClient {

    private final RestClient openAiRestClient;
    private final AppProperties properties;

    public float[] embed(String input, String model) {
        String selectedModel = model == null || model.isBlank()
                ? properties.getAi().getEmbeddingModel()
                : model;
        JsonNode response = openAiRestClient.post()
                .uri("/v1/embeddings")
                .body(Map.of("model", selectedModel, "input", List.of(input)))
                .retrieve()
                .body(JsonNode.class);
        JsonNode embeddingNode = response == null ? null : response.path("data").path(0).path("embedding");
        if (embeddingNode == null || !embeddingNode.isArray()) {
            throw new IllegalStateException("OpenAI embedding response did not include an embedding vector");
        }
        List<Float> values = new ArrayList<>();
        embeddingNode.forEach(value -> values.add((float) value.asDouble()));
        float[] embedding = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            embedding[i] = values.get(i);
        }
        return embedding;
    }
}
