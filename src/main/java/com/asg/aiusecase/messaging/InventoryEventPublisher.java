package com.asg.aiusecase.messaging;

import com.asg.aiusecase.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final AppProperties properties;

    public void publish(InventoryEvent event) {
        rabbitTemplate.convertAndSend(
                properties.getRabbit().getExchange(),
                properties.getRabbit().getRoutingKey(),
                event,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    message.getMessageProperties().setHeader("eventType", event.type().name());
                    return message;
                }
        );
        log.info("Published inventory event type={} inventoryId={} unitId={}", event.type(), event.inventoryId(), event.unitId());
    }
}
