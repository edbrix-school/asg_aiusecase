package com.asg.aiusecase.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("observability")
@ConditionalOnProperty(name = "app.observability.enabled", havingValue = "true")
public class BusinessMetrics {

    private final Counter ambiguityCounter;
    private final Counter llmResolutionCounter;

    public BusinessMetrics(MeterRegistry registry) {
        this.ambiguityCounter = Counter.builder("ags_ai_ambiguity_total")
                .description("Total ambiguous inventory search requests")
                .register(registry);
        this.llmResolutionCounter = Counter.builder("ags_ai_llm_resolution_total")
                .description("Total LLM ambiguity resolutions")
                .register(registry);
    }

    public void ambiguityDetected() {
        ambiguityCounter.increment();
    }

    public void llmResolved() {
        llmResolutionCounter.increment();
    }
}
