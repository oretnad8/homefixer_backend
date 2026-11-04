package com.homefixer.shared.kafka;

import com.homefixer.shared.dto.NotificacionEventDTO;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;

/**
 * Productor de los tópicos de HomeFixer. Requiere en el application.yml del servicio:
 * acks=all, enable.idempotence=true, JsonSerializer y spring.json.type.mapping = {@link KafkaTopics#JSON_TYPE_MAPPING}.
 *
 * Una falla de Kafka se registra como [ALERTA][KAFKA] pero no revierte la operación de negocio
 * (ya guardada y notificada por RabbitMQ): el stream analítico no debe tumbar el workflow transaccional.
 */
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String origen;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, String origen) {
        this.kafkaTemplate = kafkaTemplate;
        this.origen = origen;
    }

    public void publicarSolicitud(SolicitudEventoKafka evento) {
        enviar(KafkaTopics.SOLICITUDES_EVENTOS, evento.key(), evento, evento.eventoId(), evento.tipo().name());
    }

    /** Publica en Kafka el mismo hecho que se acaba de publicar en RabbitMQ (si es parte del stream). */
    public void publicarDesdeRabbit(String routingKey, NotificacionEventDTO dto) {
        KafkaEventMapper.desde(routingKey, dto, origen).ifPresent(this::publicarSolicitud);
    }

    public void publicarUbicacion(UbicacionTecnicoEvento evento) {
        enviar(KafkaTopics.TECNICOS_UBICACION, evento.key(), evento, evento.eventoId(), "UBICACION");
    }

    private void enviar(String topic, String key, Object payload, String eventoId, String tipo) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload);
        record.headers().add(KafkaTopics.HEADER_EVENTO_ID, eventoId.getBytes(StandardCharsets.UTF_8));
        try {
            kafkaTemplate.send(record).whenComplete((resultado, ex) -> {
                if (ex == null) {
                    RecordMetadata m = resultado.getRecordMetadata();
                    log.info("[KAFKA][PUBLISH] {} {} key={} -> partition={} offset={}",
                            topic, tipo, key, m.partition(), m.offset());
                } else {
                    log.error("[ALERTA][KAFKA] No se pudo publicar {} ({}) en {}: {}", eventoId, tipo, topic, ex.getMessage());
                }
            });
        } catch (RuntimeException e) {
            // p. ej. metadata no disponible (max.block.ms) o error de serialización
            log.error("[ALERTA][KAFKA] Error al enviar {} ({}) a {}: {}", eventoId, tipo, topic, e.getMessage());
        }
    }
}
