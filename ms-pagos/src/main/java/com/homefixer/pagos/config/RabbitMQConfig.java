package com.homefixer.pagos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homefixer.shared.messaging.EventPublisher;
import com.homefixer.shared.messaging.EventQueues;
import com.homefixer.shared.messaging.MessagingConstants;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Suscriptor de solicitud.estado-actualizado (genera el cobro al completarse un servicio),
 * publicador de pago.procesado y cliente RPC de ms-solicitudes.
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public Declarables colaPagos() {
        return EventQueues.suscriptor(MessagingConstants.PAGOS_QUEUE, MessagingConstants.PAGOS_DLQ,
                MessagingConstants.RK_SOLICITUD_ESTADO_ACTUALIZADO);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
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
