package com.homefixer.kafkaadmin.service;

import com.homefixer.kafkaadmin.dto.AdminDtos.ActualizarConfigRequest;
import com.homefixer.kafkaadmin.dto.AdminDtos.BrokerInfo;
import com.homefixer.kafkaadmin.dto.AdminDtos.ClusterInfo;
import com.homefixer.kafkaadmin.dto.AdminDtos.ConfigValor;
import com.homefixer.kafkaadmin.dto.AdminDtos.CrearTopicoRequest;
import com.homefixer.kafkaadmin.dto.AdminDtos.ParticionInfo;
import com.homefixer.kafkaadmin.dto.AdminDtos.TopicoDetalle;
import com.homefixer.kafkaadmin.dto.AdminDtos.TopicoResumen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Clúster y tópicos: listar, describir, crear, modificar configuraciones, ampliar particiones y eliminar. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaTopicosService {

    private final AdminClient admin;
    private final PoliticaAdministracion politica;

    // ---------------- Clúster ----------------

    public ClusterInfo cluster() {
        DescribeClusterResult r = admin.describeCluster();
        Node controller = KafkaFuturos.esperar(r.controller());
        List<BrokerInfo> brokers = KafkaFuturos.esperar(r.nodes()).stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(n -> new BrokerInfo(n.id(), n.host(), n.port(), n.rack(), controller != null && n.id() == controller.id()))
                .toList();
        return new ClusterInfo(KafkaFuturos.esperar(r.clusterId()), controller != null ? controller.id() : -1, brokers);
    }

    // ---------------- Lectura ----------------

    public List<TopicoResumen> listar(boolean incluirInternos) {
        Collection<String> nombres = KafkaFuturos.esperar(
                admin.listTopics(new ListTopicsOptions().listInternal(incluirInternos)).names());
        if (nombres.isEmpty()) {
            return List.of();
        }
        return describir(nombres).values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .map(d -> new TopicoResumen(d.name(), d.partitions().size(), replicas(d), d.isInternal(),
                        politica.esProtegido(d.name()), subReplicado(d)))
                .toList();
    }

    public TopicoDetalle detalle(String nombre, boolean todasLasConfigs) {
        TopicDescription d = describir(List.of(nombre)).get(nombre);
        List<TopicPartition> tps = d.partitions().stream().map(p -> new TopicPartition(nombre, p.partition())).toList();
        Map<TopicPartition, Long> inicio = offsets(tps, OffsetSpec.earliest());
        Map<TopicPartition, Long> fin = offsets(tps, OffsetSpec.latest());

        List<ParticionInfo> particiones = d.partitions().stream()
                .sorted(Comparator.comparingInt(TopicPartitionInfo::partition))
                .map(p -> {
                    TopicPartition tp = new TopicPartition(nombre, p.partition());
                    long ini = inicio.getOrDefault(tp, 0L);
                    long end = fin.getOrDefault(tp, 0L);
                    return new ParticionInfo(p.partition(), p.leader() != null ? p.leader().id() : null,
                            ids(p.replicas()), ids(p.isr()), p.isr().size() < p.replicas().size(), ini, end, end - ini);
                })
                .toList();

        return new TopicoDetalle(d.name(), d.topicId().toString(), d.isInternal(), politica.esProtegido(d.name()),
                d.partitions().size(), replicas(d), particiones.stream().mapToLong(ParticionInfo::mensajes).sum(),
                particiones, configs(nombre, todasLasConfigs));
    }

    public Map<String, ConfigValor> configs(String nombre, boolean todas) {
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = KafkaFuturos.esperar(admin.describeConfigs(List.of(recurso)).all()).get(recurso);
        Map<String, ConfigValor> resultado = new TreeMap<>();
        for (ConfigEntry e : config.entries()) {
            boolean porDefecto = e.source() == ConfigEntry.ConfigSource.DEFAULT_CONFIG
                    || e.source() == ConfigEntry.ConfigSource.STATIC_BROKER_CONFIG;
            if (todas || !porDefecto) {
                resultado.put(e.name(), new ConfigValor(e.isSensitive() ? "******" : e.value(), e.source().name(), e.isReadOnly()));
            }
        }
        return resultado;
    }

    // ---------------- Escritura ----------------

    public TopicoDetalle crear(CrearTopicoRequest req) {
        Map<String, String> configs = req.configs() != null ? req.configs() : Map.of();
        politica.validarConfigs(configs.keySet());
        configs.forEach(politica::validarValor);

        NewTopic topico = new NewTopic(req.nombre(), req.particiones(), req.replicas()).configs(configs);
        KafkaFuturos.esperar(admin.createTopics(List.of(topico)).all());
        log.info("[KAFKA-ADMIN] Tópico creado {} particiones={} replicas={} configs={}",
                req.nombre(), req.particiones(), req.replicas(), configs);
        return detalle(req.nombre(), false);
    }

    public Map<String, ConfigValor> actualizarConfigs(String nombre, ActualizarConfigRequest req) {
        Map<String, String> set = req.set() != null ? req.set() : Map.of();
        List<String> eliminar = req.eliminar() != null ? req.eliminar() : List.of();
        if (set.isEmpty() && eliminar.isEmpty()) {
            throw new IllegalArgumentException("Indique al menos una configuración en 'set' o 'eliminar'");
        }
        politica.validarConfigs(set.keySet());
        politica.validarConfigs(eliminar);
        set.forEach(politica::validarValor);
        describir(List.of(nombre)); // 404 si no existe

        List<AlterConfigOp> ops = new ArrayList<>();
        set.forEach((k, v) -> ops.add(new AlterConfigOp(new ConfigEntry(k, v), AlterConfigOp.OpType.SET)));
        eliminar.forEach(k -> ops.add(new AlterConfigOp(new ConfigEntry(k, null), AlterConfigOp.OpType.DELETE)));

        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        KafkaFuturos.esperar(admin.incrementalAlterConfigs(Map.of(recurso, ops)).all());
        log.info("[KAFKA-ADMIN] Configuración de {} actualizada set={} eliminar={}", nombre, set, eliminar);
        return configs(nombre, false);
    }

    /**
     * Kafka solo permite aumentar particiones. Cambia el mapeo key → partición: los mensajes nuevos de una
     * key pueden ir a otra partición que los antiguos (se pierde el orden entre lo viejo y lo nuevo de esa key).
     */
    public TopicoDetalle ampliarParticiones(String nombre, int total) {
        TopicDescription d = describir(List.of(nombre)).get(nombre);
        if (total <= d.partitions().size()) {
            throw new IllegalArgumentException("El tópico ya tiene " + d.partitions().size()
                    + " particiones; Kafka solo permite aumentarlas");
        }
        KafkaFuturos.esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(total))).all());
        log.warn("[KAFKA-ADMIN] Particiones de {} ampliadas de {} a {} (cambia el mapeo key -> partición)",
                nombre, d.partitions().size(), total);
        esperarParticiones(nombre, total);
        return detalle(nombre, false);
    }

    /** La metadata de las particiones nuevas tarda unos ms en propagarse a todos los brokers. */
    private void esperarParticiones(String nombre, int total) {
        for (int intento = 0; intento < 20; intento++) {
            if (describir(List.of(nombre)).get(nombre).partitions().size() >= total) {
                return;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public void eliminar(String nombre, boolean forzar) {
        politica.validarEliminacion(nombre, forzar);
        describir(List.of(nombre)); // 404 si no existe
        KafkaFuturos.esperar(admin.deleteTopics(List.of(nombre)).all());
        log.warn("[KAFKA-ADMIN] Tópico eliminado {} (forzar={})", nombre, forzar);
    }

    // ---------------- Apoyo ----------------

    Map<String, TopicDescription> describir(Collection<String> nombres) {
        return KafkaFuturos.esperar(admin.describeTopics(nombres).allTopicNames());
    }

    Map<TopicPartition, Long> offsets(Collection<TopicPartition> tps, OffsetSpec spec) {
        if (tps.isEmpty()) {
            return Map.of();
        }
        Map<TopicPartition, OffsetSpec> req = tps.stream().collect(Collectors.toMap(Function.identity(), tp -> spec));
        Map<TopicPartition, ListOffsetsResultInfo> res = KafkaFuturos.esperar(admin.listOffsets(req).all());
        return res.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().offset()));
    }

    private static int replicas(TopicDescription d) {
        return d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size();
    }

    private static boolean subReplicado(TopicDescription d) {
        return d.partitions().stream().anyMatch(p -> p.isr().size() < p.replicas().size());
    }

    private static List<Integer> ids(List<Node> nodos) {
        return nodos.stream().map(Node::id).toList();
    }
}
