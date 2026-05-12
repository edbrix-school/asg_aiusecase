package com.asg.aiusecase.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class RabbitConfig {

    private final AppProperties properties;

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }

    @Bean
    public DirectExchange inventoryExchange() {
        return new DirectExchange(properties.getRabbit().getExchange(), true, false);
    }

    @Bean
    public DirectExchange inventoryDeadLetterExchange() {
        return new DirectExchange(properties.getRabbit().getDeadLetterExchange(), true, false);
    }

    @Bean
    public Queue embeddingQueue() {
        return new Queue(properties.getRabbit().getEmbeddingQueue(), true, false, false, Map.of(
                "x-dead-letter-exchange", properties.getRabbit().getDeadLetterExchange()
        ));
    }

    @Bean
    public Queue embeddingDeadLetterQueue() {
        return new Queue(properties.getRabbit().getDeadLetterQueue(), true);
    }

    @Bean
    public Binding embeddingBinding(Queue embeddingQueue, DirectExchange inventoryExchange) {
        return BindingBuilder.bind(embeddingQueue)
                .to(inventoryExchange)
                .with(properties.getRabbit().getRoutingKey());
    }

    @Bean
    public Binding embeddingDeadLetterBinding(Queue embeddingDeadLetterQueue, DirectExchange inventoryDeadLetterExchange) {
        return BindingBuilder.bind(embeddingDeadLetterQueue)
                .to(inventoryDeadLetterExchange)
                .with("");
    }
}
