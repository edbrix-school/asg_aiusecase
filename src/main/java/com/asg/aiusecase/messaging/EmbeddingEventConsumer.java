package com.asg.aiusecase.messaging;

import com.asg.aiusecase.cache.CacheService;
import com.asg.aiusecase.config.AppProperties;
import com.asg.aiusecase.embedding.EmbeddingPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingEventConsumer {

    private final EmbeddingPipelineService embeddingPipelineService;
    private final CacheService cacheService;
    private final RabbitTemplate rabbitTemplate;
    private final AppProperties properties;

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    @RabbitListener(queues = "${app.rabbit.embedding-queue}")
    public void handle(InventoryEvent event) {
        log.info("Processing stock embedding event id={} type={}", event.eventId(), event.type());
        switch (event.type()) {
            case STOCK_CREATED, STOCK_UPDATED -> embeddingPipelineService.regenerateStockEmbedding(event.stockPoid());
            case STOCK_UNIT_CREATED, STOCK_UNIT_UPDATED -> embeddingPipelineService.regenerateStockUnitEmbedding(event.stockUnitPoid());
            case STOCK_DELETED -> embeddingPipelineService.deleteStockEmbedding(event.stockPoid());
            case STOCK_UNIT_DELETED -> embeddingPipelineService.deleteStockUnitEmbedding(event.stockUnitPoid());
        }
        cacheService.invalidateSemanticState();
    }

    @Recover
    public void recover(Exception exception, InventoryEvent event) {
        log.error("Stock embedding event failed permanently id={} type={}", event.eventId(), event.type(), exception);
        rabbitTemplate.convertAndSend(properties.getRabbit().getDeadLetterExchange(), "", event);
    }
}
