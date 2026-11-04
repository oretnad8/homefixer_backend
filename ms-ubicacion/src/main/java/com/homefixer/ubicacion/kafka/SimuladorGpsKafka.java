package com.homefixer.ubicacion.kafka;

import com.homefixer.shared.kafka.KafkaEventPublisher;
import com.homefixer.shared.kafka.UbicacionTecnicoEvento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Flujo continuo de demostración: 5 técnicos ficticios (ids 9001-9005) que se mueven por Santiago y
 * reportan su posición GPS a homefixer.tecnicos.ubicacion cada {@code homefixer.kafka.simulador.intervalo-ms}.
 * Solo publica en Kafka (no toca la base de datos ni las búsquedas reales).
 *
 * Se activa con KAFKA_SIMULADOR=true. Como la key es el tecnicoId, cada técnico cae siempre en la misma
 * partición y sus posiciones se consumen en orden; con compactación, el tópico conserva la última de cada uno.
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "homefixer.kafka.simulador.enabled", havingValue = "true")
public class SimuladorGpsKafka {

    private static final double PASO_GRADOS = 0.0015;   // ~150 m por tick

    private final ObjectProvider<KafkaEventPublisher> publisher;
    private final Map<Long, double[]> posiciones = new LinkedHashMap<>();
    private final Map<Long, Long> trabajos = new LinkedHashMap<>();

    public SimuladorGpsKafka(ObjectProvider<KafkaEventPublisher> publisher) {
        this.publisher = publisher;
        // Providencia, Ñuñoa, Las Condes, Santiago Centro, Maipú
        posiciones.put(9001L, new double[]{-33.4263, -70.6110});
        posiciones.put(9002L, new double[]{-33.4569, -70.5978});
        posiciones.put(9003L, new double[]{-33.4103, -70.5672});
        posiciones.put(9004L, new double[]{-33.4489, -70.6693});
        posiciones.put(9005L, new double[]{-33.5110, -70.7580});
        log.info("[KAFKA][SIMULADOR] Simulador GPS activo para técnicos {}", posiciones.keySet());
    }

    @Scheduled(fixedRateString = "${homefixer.kafka.simulador.intervalo-ms:2000}", initialDelay = 5000)
    public void publicarPosiciones() {
        KafkaEventPublisher kafka = publisher.getIfAvailable();
        if (kafka == null) {
            return; // homefixer.kafka.enabled=false
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        posiciones.forEach((tecnicoId, pos) -> {
            pos[0] += rnd.nextDouble(-PASO_GRADOS, PASO_GRADOS);
            pos[1] += rnd.nextDouble(-PASO_GRADOS, PASO_GRADOS);
            // De vez en cuando un técnico toma o termina un trabajo: métrica de ocupados vs. disponibles
            if (rnd.nextInt(20) == 0) {
                if (trabajos.containsKey(tecnicoId)) {
                    trabajos.remove(tecnicoId);
                } else {
                    trabajos.put(tecnicoId, 90_000L + rnd.nextInt(1000));
                }
            }
            kafka.publicarUbicacion(new UbicacionTecnicoEvento(UUID.randomUUID().toString(), tecnicoId,
                    pos[0], pos[1], true, trabajos.get(tecnicoId), Instant.now(), "simulador"));
        });
    }
}
