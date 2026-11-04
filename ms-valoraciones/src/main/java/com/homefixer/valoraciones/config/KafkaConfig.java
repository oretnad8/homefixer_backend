package com.homefixer.valoraciones.config;

import com.homefixer.shared.kafka.KafkaEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Productor Kafka de ms-valoraciones: publica VALORACION_REGISTRADA en homefixer.solicitudes.eventos.
 * Cliente (bootstrap, serializers, acks, idempotencia) configurado en spring.kafka del application.yml.
 * Con homefixer.kafka.enabled=false no se crea el publicador y el servicio funciona solo con RabbitMQ.
 */
@Configuration
@ConditionalOnProperty(name = "homefixer.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

    @Bean
    public KafkaEventPublisher kafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                                   @Value("${spring.application.name}") String aplicacion) {
        return new KafkaEventPublisher(kafkaTemplate, aplicacion);
    }
}
