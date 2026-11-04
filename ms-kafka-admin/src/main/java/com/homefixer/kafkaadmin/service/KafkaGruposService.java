package com.homefixer.kafkaadmin.service;

import com.homefixer.kafkaadmin.dto.AdminDtos.GrupoDetalle;
import com.homefixer.kafkaadmin.dto.AdminDtos.GrupoResumen;
import com.homefixer.kafkaadmin.dto.AdminDtos.LagParticion;
import com.homefixer.kafkaadmin.dto.AdminDtos.MiembroGrupo;
import com.homefixer.kafkaadmin.dto.AdminDtos.ResetOffsetsRequest;
import com.homefixer.kafkaadmin.dto.AdminDtos.ResultadoReset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Consumer groups: listar, describir con lag por partición (offset final - offset comprometido),
 * reset de offsets (replay / saltar backlog) y eliminación.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaGruposService {

    private final AdminClient admin;
    private final KafkaTopicosService topicos;

    public List<GrupoResumen> listar() {
        return KafkaFuturos.esperar(admin.listConsumerGroups().all()).stream()
                .map(g -> new GrupoResumen(g.groupId(), g.state().map(Enum::name).orElse("UNKNOWN"), g.isSimpleConsumerGroup()))
                .sorted(Comparator.comparing(GrupoResumen::grupo))
                .toList();
    }

    public GrupoDetalle detalle(String grupo) {
        ConsumerGroupDescription d = describir(grupo);
        Map<TopicPartition, OffsetAndMetadata> comprometidos = comprometidos(grupo);

        // Partición -> consumidor asignado (incluye particiones asignadas que aún no tienen commit)
        Map<TopicPartition, String> asignacion = new HashMap<>();
        List<MiembroGrupo> miembros = d.members().stream()
                .sorted(Comparator.comparing(MemberDescription::clientId))
                .map(m -> {
                    m.assignment().topicPartitions().forEach(tp -> asignacion.put(tp, m.consumerId()));
                    return new MiembroGrupo(m.consumerId(), m.clientId(), m.host(),
                            m.assignment().topicPartitions().stream()
                                    .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                                    .map(TopicPartition::toString).toList());
                })
                .toList();

        Set<TopicPartition> todas = new HashSet<>(comprometidos.keySet());
        todas.addAll(asignacion.keySet());
        Map<TopicPartition, Long> finales = topicos.offsets(todas, OffsetSpec.latest());

        Map<String, Long> lagPorTopico = new TreeMap<>();
        List<LagParticion> particiones = todas.stream()
                .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                .map(tp -> {
                    OffsetAndMetadata c = comprometidos.get(tp);
                    long fin = finales.getOrDefault(tp, 0L);
                    Long lag = c != null ? Math.max(0, fin - c.offset()) : null;
                    if (lag != null) {
                        lagPorTopico.merge(tp.topic(), lag, Long::sum);
                    }
                    return new LagParticion(tp.topic(), tp.partition(), c != null ? c.offset() : null, fin, lag,
                            asignacion.get(tp));
                })
                .toList();

        return new GrupoDetalle(grupo, d.state().name(), d.partitionAssignor(),
                d.coordinator() != null ? d.coordinator().id() : null, miembros,
                lagPorTopico.values().stream().mapToLong(Long::longValue).sum(), lagPorTopico, particiones);
    }

    /** Solo con el grupo sin consumidores activos (estado EMPTY o DEAD), igual que kafka-consumer-groups --reset-offsets. */
    public ResultadoReset resetOffsets(String grupo, ResetOffsetsRequest req) {
        ConsumerGroupDescription d = describir(grupo);
        if (d.state() != ConsumerGroupState.EMPTY && d.state() != ConsumerGroupState.DEAD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El grupo " + grupo + " está " + d.state()
                    + " con " + d.members().size() + " consumidor(es) activos: deténgalos antes de mover los offsets");
        }
        Set<TopicPartition> particiones = particionesObjetivo(grupo, req.topicos());
        if (particiones.isEmpty()) {
            throw new IllegalArgumentException("El grupo no tiene offsets; indique 'topicos'");
        }

        Map<TopicPartition, Long> destino = switch (req.estrategia()) {
            case EARLIEST -> topicos.offsets(particiones, OffsetSpec.earliest());
            case LATEST -> topicos.offsets(particiones, OffsetSpec.latest());
            case TIMESTAMP -> porTimestamp(particiones, req);
            case OFFSET -> porOffset(particiones, req);
        };

        Map<TopicPartition, OffsetAndMetadata> nuevos = new LinkedHashMap<>();
        destino.forEach((tp, off) -> nuevos.put(tp, new OffsetAndMetadata(off)));
        KafkaFuturos.esperar(admin.alterConsumerGroupOffsets(grupo, nuevos).all());
        log.warn("[KAFKA-ADMIN] Offsets de {} movidos con {} en {} particiones", grupo, req.estrategia(), nuevos.size());

        Map<TopicPartition, Long> finales = topicos.offsets(nuevos.keySet(), OffsetSpec.latest());
        List<LagParticion> resultado = nuevos.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition)))
                .map(e -> {
                    long fin = finales.getOrDefault(e.getKey(), 0L);
                    return new LagParticion(e.getKey().topic(), e.getKey().partition(), e.getValue().offset(), fin,
                            Math.max(0, fin - e.getValue().offset()), null);
                })
                .toList();
        return new ResultadoReset(grupo, req.estrategia(), resultado);
    }

    public void eliminar(List<String> grupos) {
        KafkaFuturos.esperar(admin.deleteConsumerGroups(grupos).all());
        log.warn("[KAFKA-ADMIN] Consumer groups eliminados {}", grupos);
    }

    // ---------------- Apoyo ----------------

    private ConsumerGroupDescription describir(String grupo) {
        ConsumerGroupDescription d = KafkaFuturos.esperar(admin.describeConsumerGroups(List.of(grupo)).all()).get(grupo);
        if (d == null || (d.state() == ConsumerGroupState.DEAD && comprometidos(grupo).isEmpty())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El consumer group " + grupo + " no existe");
        }
        return d;
    }

    private Map<TopicPartition, OffsetAndMetadata> comprometidos(String grupo) {
        Map<TopicPartition, OffsetAndMetadata> m =
                KafkaFuturos.esperar(admin.listConsumerGroupOffsets(grupo).partitionsToOffsetAndMetadata());
        Map<TopicPartition, OffsetAndMetadata> limpio = new HashMap<>();
        m.forEach((tp, o) -> {
            if (o != null) {
                limpio.put(tp, o);
            }
        });
        return limpio;
    }

    private Set<TopicPartition> particionesObjetivo(String grupo, List<String> nombres) {
        if (nombres == null || nombres.isEmpty()) {
            return comprometidos(grupo).keySet();
        }
        Set<TopicPartition> tps = new HashSet<>();
        for (TopicDescription d : topicos.describir(nombres).values()) {
            d.partitions().forEach(p -> tps.add(new TopicPartition(d.name(), p.partition())));
        }
        return tps;
    }

    private Map<TopicPartition, Long> porTimestamp(Set<TopicPartition> tps, ResetOffsetsRequest req) {
        if (req.timestamp() == null) {
            throw new IllegalArgumentException("La estrategia TIMESTAMP requiere 'timestamp' (ISO-8601)");
        }
        Map<TopicPartition, Long> porTiempo = topicos.offsets(tps, OffsetSpec.forTimestamp(req.timestamp().toEpochMilli()));
        Map<TopicPartition, Long> finales = topicos.offsets(tps, OffsetSpec.latest());
        Map<TopicPartition, Long> destino = new HashMap<>();
        // -1 = no hay mensajes posteriores a ese instante: se posiciona al final
        tps.forEach(tp -> destino.put(tp, porTiempo.getOrDefault(tp, -1L) < 0 ? finales.get(tp) : porTiempo.get(tp)));
        return destino;
    }

    private Map<TopicPartition, Long> porOffset(Set<TopicPartition> tps, ResetOffsetsRequest req) {
        if (req.offset() == null) {
            throw new IllegalArgumentException("La estrategia OFFSET requiere 'offset'");
        }
        Map<TopicPartition, Long> inicio = topicos.offsets(tps, OffsetSpec.earliest());
        Map<TopicPartition, Long> fin = topicos.offsets(tps, OffsetSpec.latest());
        Map<TopicPartition, Long> destino = new HashMap<>();
        tps.forEach(tp -> destino.put(tp, Math.max(inicio.get(tp), Math.min(fin.get(tp), req.offset()))));
        return destino;
    }
}
