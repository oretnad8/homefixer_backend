package com.homefixer.reportes.kafka;

import com.homefixer.reportes.service.MetricasStreamingService;
import com.homefixer.shared.kafka.KafkaTopics;
import com.homefixer.shared.kafka.SolicitudEventoKafka;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Infraestructura de mensajería: recibe los eventos de negocio y delega en {@link MetricasStreamingService}
 * (dominio, sin dependencias de Kafka). Si el payload es inválido, {@code validar()} lanza
 * EventoInvalidoException, que el error handler envía directo al DLT sin reintentar.
 *
 * concurrency=3: un hilo por partición. Con 2 instancias de ms-reportes en el mismo grupo,
 * las 3 particiones se reparten entre ellas (balanceo) y, si una cae, la otra las toma (rebalanceo).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitudEventosKafkaListener {

    private final MetricasStreamingService metricas;

    @KafkaListener(id = "reportes-solicitudes-eventos",
            topics = KafkaTopics.SOLICITUDES_EVENTOS,
            groupId = KafkaTopics.GROUP_REPORTES_METRICAS,
            concurrency = "3",
            // Tipo por defecto si el mensaje no trae header __TypeId__ (p. ej. publicado a mano desde Kafka UI)
            properties = "spring.json.value.default.type=com.homefixer.shared.kafka.SolicitudEventoKafka")
    public void onEvento(@Payload SolicitudEventoKafka evento,
                         @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                         @Header(KafkaHeaders.OFFSET) long offset) {
        evento.validar();
        log.info("[KAFKA][CONSUME] {} solicitud={} estado={} partition={} offset={}",
                evento.tipo(), evento.solicitudId(), evento.estadoNuevo(), partition, offset);
        if (!metricas.registrarEvento(evento)) {
            log.info("[KAFKA][DUPLICADO] Evento {} ya procesado, se ignora", evento.eventoId());
        }
    }
}
