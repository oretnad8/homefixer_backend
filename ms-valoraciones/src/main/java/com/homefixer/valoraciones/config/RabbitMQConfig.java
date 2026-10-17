package com.homefixer.valoraciones.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homefixer.shared.messaging.EventPublisher;
import com.homefixer.shared.messaging.EventQueues;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Publicador de valoracion.registrada y cliente RPC de ms-solicitudes. */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange eventsExchange() {
        return EventQueues.eventsExchange();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RabbitTemplate template = EventPublisher.rabbitTemplate(connectionFactory, objectMapper);
        template.setReplyTimeout(5000);
        return template;
    }

    @Bean
    public EventPublisher eventPublisher(RabbitTemplate rabbitTemplate) {
        return new EventPublisher(rabbitTemplate);
    }
}
