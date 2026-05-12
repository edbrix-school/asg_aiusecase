package com.asg.aiusecase.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final ObjectProvider<EmbeddingModel> springAiEmbeddingModel;
    private final OpenAiEmbeddingHttpClient openAiEmbeddingHttpClient;

    public float[] embed(String text, String modelOverride) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text for embedding must not be blank");
        }
        if (modelOverride != null && !modelOverride.isBlank()) {
            return openAiEmbeddingHttpClient.embed(text, modelOverride);
        }
        EmbeddingModel model = springAiEmbeddingModel.getIfAvailable();
        if (model != null) {
            return model.embed(text);
        }
        return openAiEmbeddingHttpClient.embed(text, null);
    }
}
