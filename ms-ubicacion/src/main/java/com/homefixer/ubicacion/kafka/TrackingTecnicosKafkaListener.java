package com.homefixer.ubicacion.kafka;

import com.homefixer.shared.kafka.KafkaTopics;
import com.homefixer.shared.kafka.UbicacionTecnicoEvento;
import com.homefixer.ubicacion.service.TrackingTecnicosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumer group ubicacion-tracking sobre homefixer.tecnicos.ubicacion. Recibe todas las posiciones
 * (de la app real y del simulador), mantiene el seguimiento y cada 30 s revisa técnicos inactivos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "homefixer.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TrackingTecnicosKafkaListener {

    private final TrackingTecnicosService tracking;

    @KafkaListener(id = "ubicacion-tracking-gps",
            topics = KafkaTopics.TECNICOS_UBICACION,
            groupId = KafkaTopics.GROUP_UBICACION_TRACKING,
            concurrency = "3",
            properties = "spring.json.value.default.type=com.homefixer.shared.kafka.UbicacionTecnicoEvento")
    public void onUbicacion(@Payload UbicacionTecnicoEvento evento,
                            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                            @Header(KafkaHeaders.OFFSET) long offset) {
        evento.validar();
        log.debug("[KAFKA][TRACKING] tecnico={} enLinea={} p={} o={}", evento.tecnicoId(), evento.enLinea(), partition, offset);
        tracking.registrar(evento);
    }

    @Scheduled(fixedRate = 30_000, initialDelay = 30_000)
    public void revisarInactivos() {
        tracking.detectarInactivos(Instant.now()).forEach(id ->
                log.warn("[ALERTA][TRACKING] Técnico {} en línea sin reportar posición dentro del umbral", id));
    }
}
