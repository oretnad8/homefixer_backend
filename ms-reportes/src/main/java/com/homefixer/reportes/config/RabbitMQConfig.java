package com.homefixer.reportes.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homefixer.shared.messaging.MessagingConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Segundo suscriptor de los eventos de solicitudes: cada evento publicado llega tanto a
 * ms-notificacion como a ms-reportes, cada uno en su propia cola (Publish/Subscribe).
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange eventsExchange() {
        return ExchangeBuilder.topicExchange(MessagingConstants.EVENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(MessagingConstants.DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue reportesQueue() {
        return QueueBuilder.durable(MessagingConstants.REPORTES_QUEUE)
                .deadLetterExchange(MessagingConstants.DLX_EXCHANGE)
                .deadLetterRoutingKey(MessagingConstants.REPORTES_DLQ)
                .ttl(MessagingConstants.MAIN_QUEUE_TTL_MS)
                .maxLength(MessagingConstants.MAIN_QUEUE_MAX_LENGTH)
                .withArgument("x-overflow", "reject-publish-dlx")
                .build();
    }

    @Bean
    public Queue reportesDeadLetterQueue() {
        return QueueBuilder.durable(MessagingConstants.REPORTES_DLQ)
                .ttl(MessagingConstants.DLQ_TTL_MS)
                .maxLength(MessagingConstants.DLQ_MAX_LENGTH)
                .build();
    }

    @Bean
    public Binding reportesBinding() {
        return BindingBuilder.bind(reportesQueue()).to(eventsExchange()).with(MessagingConstants.RK_SOLICITUD_ALL);
    }

    @Bean
    public Binding reportesDeadLetterBinding() {
        return BindingBuilder.bind(reportesDeadLetterQueue()).to(deadLetterExchange())
                .with(MessagingConstants.REPORTES_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
