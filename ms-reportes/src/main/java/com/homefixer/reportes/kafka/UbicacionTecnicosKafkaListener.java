package com.homefixer.reportes.kafka;

import com.homefixer.reportes.service.MetricasStreamingService;
import com.homefixer.shared.kafka.KafkaTopics;
import com.homefixer.shared.kafka.UbicacionTecnicoEvento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/** Stream GPS de técnicos: técnicos en línea / ocupados y actualizaciones por minuto. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UbicacionTecnicosKafkaListener {

    private final MetricasStreamingService metricas;

    @KafkaListener(id = "reportes-tecnicos-ubicacion",
            topics = KafkaTopics.TECNICOS_UBICACION,
            groupId = KafkaTopics.GROUP_REPORTES_METRICAS,
            concurrency = "3",
            properties = "spring.json.value.default.type=com.homefixer.shared.kafka.UbicacionTecnicoEvento")
    public void onUbicacion(@Payload UbicacionTecnicoEvento evento,
                            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                            @Header(KafkaHeaders.OFFSET) long offset) {
        evento.validar();
        log.debug("[KAFKA][CONSUME] UBICACION tecnico={} enLinea={} partition={} offset={}",
                evento.tecnicoId(), evento.enLinea(), partition, offset);
        metricas.registrarUbicacion(evento);
    }
}
