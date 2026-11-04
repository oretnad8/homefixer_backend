package com.homefixer.notificacion.listener;

import com.homefixer.notificacion.service.AuditoriaEventosService;
import com.homefixer.notificacion.service.AuditoriaEventosService.Anomalia;
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
 * Consumer group notificacion-auditoria sobre homefixer.solicitudes.eventos. Lee el mismo tópico que
 * ms-reportes (reportes-metricas) pero con offsets propios: cada grupo recibe todos los eventos
 * (pub/sub entre grupos), y dentro de un grupo las particiones se reparten (balanceo).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaEventosKafkaListener {

    private final AuditoriaEventosService auditoria;

    @KafkaListener(id = "notificacion-auditoria-eventos",
            topics = KafkaTopics.SOLICITUDES_EVENTOS,
            groupId = KafkaTopics.GROUP_NOTIFICACION_AUDITORIA,
            concurrency = "3",
            properties = "spring.json.value.default.type=com.homefixer.shared.kafka.SolicitudEventoKafka")
    public void onEvento(@Payload SolicitudEventoKafka evento,
                         @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                         @Header(KafkaHeaders.OFFSET) long offset) {
        evento.validar();
        Anomalia anomalia = auditoria.registrar(evento);
        log.debug("[KAFKA][AUDITORIA] {} solicitud={} p={} o={}", evento.tipo(), evento.solicitudId(), partition, offset);
        if (anomalia != null) {
            log.warn("[ALERTA][AUDITORIA] solicitud={} evento={} : {}", anomalia.solicitudId(), anomalia.eventoId(), anomalia.detalle());
        }
    }
}
