package com.asg.aiusecase.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiClientConfig {

    @Bean
    public RestClient openAiRestClient(RestClient.Builder builder, AppProperties properties) {
        String apiKey = properties.getAi().getApiKey();
        RestClient.Builder configured = builder
                .baseUrl(properties.getAi().getOpenaiBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        if (apiKey != null && !apiKey.isBlank()) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        return configured.build();
    }

    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }
}
