package com.homefixer.maestria.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homefixer.shared.messaging.EventPublisher;
import com.homefixer.shared.messaging.EventQueues;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Publicador de certificacion.aprobada / certificacion.rechazada (los notifica ms-notificacion). */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange eventsExchange() {
        return EventQueues.eventsExchange();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        return EventPublisher.rabbitTemplate(connectionFactory, objectMapper);
    }

    @Bean
    public EventPublisher eventPublisher(RabbitTemplate rabbitTemplate) {
        return new EventPublisher(rabbitTemplate);
    }
}
