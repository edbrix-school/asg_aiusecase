package com.asg.aiusecase.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("observability")
@ConditionalOnProperty(name = "app.observability.enabled", havingValue = "true")
public class OptionalObservabilityConfig {

    public OptionalObservabilityConfig() {
        log.info("Optional observability profile enabled");
    }
}
