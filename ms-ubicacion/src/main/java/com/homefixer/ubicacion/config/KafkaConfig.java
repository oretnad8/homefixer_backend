package com.homefixer.ubicacion.config;

import com.homefixer.shared.kafka.KafkaErrorHandling;
import com.homefixer.shared.kafka.KafkaEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Kafka en ms-ubicacion:
 * <ul>
 *   <li>Productor: cada reporte GPS se publica en homefixer.tecnicos.ubicacion (key = tecnicoId).</li>
 *   <li>Consumidor (group ubicacion-tracking): seguimiento de técnicos desde el stream y alerta de inactividad.</li>
 * </ul>
 * Cliente configurado en spring.kafka del application.yml. Con homefixer.kafka.enabled=false no se publica
 * ni se consume; el servicio funciona solo con la base de datos.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "homefixer.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

    @Bean
    public KafkaEventPublisher kafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                                   @Value("${spring.application.name}") String aplicacion) {
        return new KafkaEventPublisher(kafkaTemplate, aplicacion);
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(ProducerFactory<?, ?> producerFactory) {
        return KafkaErrorHandling.errorHandler(producerFactory);
    }
}
