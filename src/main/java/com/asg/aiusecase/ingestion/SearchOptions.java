package com.asg.aiusecase.ingestion;

public record SearchOptions(
        Integer topK,
        Double minSimilarity,
        String embeddingModel,
        String chatModel,
        String fallbackModel,
        Boolean allowLlm,
        Boolean includeCandidates,
        Boolean llmParseQuantity,
        String llmParseModel
) {
    public static SearchOptions empty() {
        return new SearchOptions(null, null, null, null, null, null, null, null, null);
    }

    public SearchOptions merge(SearchOptions defaults) {
        if (defaults == null) {
            return this;
        }
        return new SearchOptions(
                topK != null ? topK : defaults.topK,
                minSimilarity != null ? minSimilarity : defaults.minSimilarity,
                embeddingModel != null ? embeddingModel : defaults.embeddingModel,
                chatModel != null ? chatModel : defaults.chatModel,
                fallbackModel != null ? fallbackModel : defaults.fallbackModel,
                allowLlm != null ? allowLlm : defaults.allowLlm,
                includeCandidates != null ? includeCandidates : defaults.includeCandidates,
                llmParseQuantity != null ? llmParseQuantity : defaults.llmParseQuantity,
                llmParseModel != null ? llmParseModel : defaults.llmParseModel
        );
    }
}
