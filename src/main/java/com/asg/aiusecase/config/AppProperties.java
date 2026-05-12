package com.asg.aiusecase.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Ai ai = new Ai();
    private Matching matching = new Matching();
    private Cache cache = new Cache();
    private Rabbit rabbit = new Rabbit();
    private Security security = new Security();
    private Observability observability = new Observability();

    @Getter
    @Setter
    public static class Ai {
        private String openaiBaseUrl = "https://api.openai.com";
        private String apiKey = "";
        private String embeddingModel = "text-embedding-3-small";
        private String chatModel = "gpt-5.5";
        private String fallbackModel = "gpt-5.4-mini";
        private String reasoningEffort = "low";
        private int embeddingDimensions = 1536;
        private Duration timeout = Duration.ofSeconds(45);
    }

    @Getter
    @Setter
    public static class Matching {
        private int topK = 8;
        private double similarityThreshold = 0.60;
        private double highConfidenceThreshold = 0.86;
        private double ambiguityDelta = 0.08;
        private boolean llmEnabled = true;
        private int maxLlmCandidates = 5;
    }

    @Getter
    @Setter
    public static class Cache {
        private Duration finalTtl = Duration.ofMinutes(30);
        private Duration semanticTtl = Duration.ofHours(6);
        private Duration vectorTtl = Duration.ofMinutes(15);
        private Duration llmTtl = Duration.ofHours(24);
        private String keyPrefix = "ags-ai";
    }

    @Getter
    @Setter
    public static class Rabbit {
        private String exchange = "ags.ai.inventory.exchange";
        private String routingKey = "ags.ai.inventory.embedding";
        private String embeddingQueue = "ags.ai.inventory.embedding.queue";
        private String deadLetterExchange = "ags.ai.inventory.dlx";
        private String deadLetterQueue = "ags.ai.inventory.embedding.dlq";
        private int retryAttempts = 3;
        private long retryDelayMs = 2000;
    }

    @Getter
    @Setter
    public static class Security {
        private boolean enabled = false;
        private String apiKey = "";
    }

    @Getter
    @Setter
    public static class Observability {
        private boolean enabled = false;
    }
}
