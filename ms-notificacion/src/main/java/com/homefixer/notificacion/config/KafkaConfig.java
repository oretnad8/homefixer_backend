package com.homefixer.notificacion.config;

import com.homefixer.shared.kafka.KafkaErrorHandling;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;

/**
 * Consumidor Kafka de ms-notificacion (consumer group notificacion-auditoria).
 * Las notificaciones al usuario siguen llegando por RabbitMQ; Kafka se usa para auditar el stream de
 * eventos de negocio, leyendo el mismo tópico que ms-reportes pero con offsets propios.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(ProducerFactory<?, ?> producerFactory) {
        return KafkaErrorHandling.errorHandler(producerFactory);
    }
}
