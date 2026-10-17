package com.homefixer.usuarios.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homefixer.shared.messaging.EventQueues;
import com.homefixer.shared.messaging.MessagingConstants;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Suscriptor de valoracion.registrada: mantiene la calificación promedio de los técnicos. */
@Configuration
public class RabbitMQConfig {

    @Bean
    public Declarables colaValoraciones() {
        return EventQueues.suscriptor(MessagingConstants.USUARIOS_VALORACIONES_QUEUE,
                MessagingConstants.USUARIOS_VALORACIONES_DLQ, MessagingConstants.RK_VALORACION_REGISTRADA);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
