package com.homefixer.reportes.config;

import com.homefixer.shared.kafka.KafkaErrorHandling;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Consumidor Kafka de ms-reportes (consumer group reportes-metricas, ambos tópicos).
 * Spring Boot aplica este CommonErrorHandler a todos los @KafkaListener del servicio:
 * 3 reintentos con backoff exponencial y luego envío a {@code <topic>.DLT}.
 * La configuración del cliente (deserializers, group, commit manual, assignor) está en spring.kafka del application.yml.
 */
@Configuration
@EnableScheduling   // empuje periódico de métricas por SSE
public class KafkaConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(ProducerFactory<?, ?> producerFactory) {
        return KafkaErrorHandling.errorHandler(producerFactory);
    }
}
