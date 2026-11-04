package com.homefixer.kafkaadmin.service;

import com.homefixer.kafkaadmin.dto.AdminDtos.ResultadoSimulacion;
import com.homefixer.shared.kafka.KafkaEventPublisher;
import com.homefixer.shared.kafka.KafkaTopics;
import com.homefixer.shared.kafka.SolicitudEventoKafka;
import com.homefixer.shared.kafka.TipoEventoKafka;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generador de carga para la demo de supervisión (EP6): publica N eventos de ciclo de vida completos
 * (CREADA → ACEPTADA → EN_PROCESO → COMPLETADA → PAGADA → valorada) para solicitudes ficticias
 * (ids desde 1.000.000, origen "simulador-carga"). Con ms-reportes detenido se ve el lag crecer en
 * Kafka UI / GET /consumer-groups/reportes-metricas, y bajar al levantar 1 o 2 instancias.
 */
@Slf4j
@Service
public class SimuladorCargaService {

    private static final List<String> CATEGORIAS = List.of("plomeria", "electricidad", "carpinteria", "pintura", "cerrajeria", "limpieza");
    private static final String ORIGEN = "simulador-carga";

    private final KafkaEventPublisher publisher;
    private final int maxEventos;

    public SimuladorCargaService(KafkaEventPublisher publisher,
                                 @Value("${homefixer.kafka.admin.max-eventos-simulacion:20000}") int maxEventos) {
        this.publisher = publisher;
        this.maxEventos = maxEventos;
    }

    public ResultadoSimulacion simular(int eventos) {
        if (eventos < 1 || eventos > maxEventos) {
            throw new IllegalArgumentException("eventos debe estar entre 1 y " + maxEventos);
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int enviados = 0;
        while (enviados < eventos) {
            long solicitudId = 1_000_000L + rnd.nextLong(9_000_000L);
            long clienteId = 500L + rnd.nextInt(100);
            long tecnicoId = 9001L + rnd.nextInt(5);
            String categoria = CATEGORIAS.get(rnd.nextInt(CATEGORIAS.size()));
            double monto = 15_000 + rnd.nextInt(30) * 1_000;

            // Todos los eventos de una solicitud comparten key → misma partición → orden garantizado
            enviados += emitir(eventos - enviados, solicitudId, clienteId, tecnicoId, categoria, monto, rnd);
        }
        log.info("[KAFKA-ADMIN][SIMULADOR] {} eventos enviados a {}", enviados, KafkaTopics.SOLICITUDES_EVENTOS);
        return new ResultadoSimulacion(enviados, KafkaTopics.SOLICITUDES_EVENTOS,
                "Eventos encolados; revise el lag del grupo " + KafkaTopics.GROUP_REPORTES_METRICAS);
    }

    private int emitir(int restantes, long sol, long cli, long tec, String cat, double monto, ThreadLocalRandom rnd) {
        Object[][] ciclo = {
                {TipoEventoKafka.SOLICITUD_CREADA, null, "CREADA", null},
                {TipoEventoKafka.TECNICO_ASIGNADO, "CREADA", "ACEPTADA", null},
                {TipoEventoKafka.ESTADO_ACTUALIZADO, "ACEPTADA", "EN_PROCESO", null},
                {TipoEventoKafka.ESTADO_ACTUALIZADO, "EN_PROCESO", "COMPLETADA", null},
                {TipoEventoKafka.PAGO_PROCESADO, "COMPLETADA", "PAGADA", null},
                {TipoEventoKafka.VALORACION_REGISTRADA, "PAGADA", "PAGADA", 3.5 + rnd.nextInt(4) * 0.5},
        };
        int n = Math.min(restantes, ciclo.length);
        for (int i = 0; i < n; i++) {
            Object[] paso = ciclo[i];
            TipoEventoKafka tipo = (TipoEventoKafka) paso[0];
            publisher.publicarSolicitud(new SolicitudEventoKafka(UUID.randomUUID().toString(), tipo,
                    SolicitudEventoKafka.VERSION_ACTUAL, Instant.now(), ORIGEN, sol, cli,
                    tipo == TipoEventoKafka.SOLICITUD_CREADA ? null : tec, cat,
                    (String) paso[1], (String) paso[2], monto, (Double) paso[3]));
        }
        return n;
    }
}
