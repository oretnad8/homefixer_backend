package com.homefixer.solicitudes.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homefixer.shared.messaging.EventPublisher;
import com.homefixer.shared.messaging.EventQueues;
import com.homefixer.shared.messaging.MessagingConstants;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Productor de eventos (solicitud.*, chat.*), suscriptor de pago.procesado y servidor RPC
 * (resumen y detalle de solicitudes). Las colas de los demás suscriptores las declara cada consumidor.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange eventsExchange() {
        return EventQueues.eventsExchange();
    }

    @Bean
    public Declarables colaPagos() {
        return EventQueues.suscriptor(MessagingConstants.SOLICITUDES_PAGOS_QUEUE, MessagingConstants.SOLICITUDES_PAGOS_DLQ,
                MessagingConstants.RK_PAGO_PROCESADO);
    }

    @Bean
    public Declarables rpcResumen() {
        return EventQueues.colaRpc(MessagingConstants.RPC_RESUMEN_SOLICITUDES_QUEUE, MessagingConstants.RK_RPC_RESUMEN_SOLICITUDES);
    }

    @Bean
    public Declarables rpcDetalle() {
        return EventQueues.colaRpc(MessagingConstants.RPC_DETALLE_SOLICITUD_QUEUE, MessagingConstants.RK_RPC_DETALLE_SOLICITUD);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
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
