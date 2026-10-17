package com.homefixer.notificacion.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homefixer.shared.messaging.EventQueues;
import com.homefixer.shared.messaging.MessagingConstants;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Suscriptor de todos los eventos de dominio (Publish/Subscribe) con su propia cola durable,
 * Dead Letter Exchange y políticas de retención (ver EventQueues).
 */
@Configuration
public class RabbitMQConfig {

    @Bean
    public Declarables colaNotificaciones() {
        return EventQueues.suscriptor(MessagingConstants.NOTIFICACION_QUEUE, MessagingConstants.NOTIFICACION_DLQ,
                MessagingConstants.RK_SOLICITUD_ALL, MessagingConstants.RK_PAGO_ALL,
                MessagingConstants.RK_VALORACION_ALL, MessagingConstants.RK_CHAT_ALL,
                MessagingConstants.RK_CERTIFICACION_ALL);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
