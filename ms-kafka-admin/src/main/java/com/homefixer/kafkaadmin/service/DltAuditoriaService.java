package com.homefixer.kafkaadmin.service;

import com.homefixer.kafkaadmin.dto.AdminDtos.MensajeDlt;
import com.homefixer.shared.kafka.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Auditoría de los Dead Letter Topics (consumer group dlt-auditoria): registra cada mensaje fallido
 * con su causa y origen ([DLT] en el log) y conserva los últimos 200 para GET /api/kafka/admin/dlt.
 * Los valores se leen como texto (StringDeserializer) porque pueden ser JSON corrupto.
 */
@Slf4j
@Service
public class DltAuditoriaService {

    private static final int MAX = 200;
    private final Deque<MensajeDlt> ultimos = new ArrayDeque<>();

    @KafkaListener(id = "kafka-admin-dlt", groupId = KafkaTopics.GROUP_DLT_AUDITORIA,
            topics = {KafkaTopics.SOLICITUDES_EVENTOS_DLT, KafkaTopics.TECNICOS_UBICACION_DLT})
    public void onDlt(ConsumerRecord<String, String> r) {
        MensajeDlt m = new MensajeDlt(r.topic(), r.partition(), r.offset(), r.key(), r.value(),
                texto(r, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                entero(r, KafkaHeaders.DLT_ORIGINAL_PARTITION),
                largo(r, KafkaHeaders.DLT_ORIGINAL_OFFSET),
                texto(r, KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP),
                texto(r, KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN),
                primeraLinea(texto(r, KafkaHeaders.DLT_EXCEPTION_MESSAGE)),
                Instant.ofEpochMilli(r.timestamp()));
        log.warn("[DLT] {} p={} o={} key={} origen={}[{}]@{} grupo={} causa={} : {}", r.topic(), r.partition(), r.offset(),
                r.key(), m.topicoOriginal(), m.particionOriginal(), m.offsetOriginal(), m.grupoOriginal(),
                m.excepcion(), m.mensajeError());
        synchronized (ultimos) {
            ultimos.addFirst(m);
            while (ultimos.size() > MAX) {
                ultimos.removeLast();
            }
        }
    }

    public List<MensajeDlt> ultimos(String topico, int limite) {
        synchronized (ultimos) {
            return ultimos.stream()
                    .filter(m -> topico == null || m.topico().equals(topico) || topico.equals(m.topicoOriginal()))
                    .limit(Math.max(1, Math.min(limite, MAX)))
                    .toList();
        }
    }

    private static String texto(ConsumerRecord<?, ?> r, String nombre) {
        Header h = r.headers().lastHeader(nombre);
        return h != null ? new String(h.value(), StandardCharsets.UTF_8) : null;
    }

    /** kafka_dlt-original-partition es un int binario (4 bytes). */
    private static String entero(ConsumerRecord<?, ?> r, String nombre) {
        Header h = r.headers().lastHeader(nombre);
        return h != null && h.value().length == Integer.BYTES ? String.valueOf(ByteBuffer.wrap(h.value()).getInt()) : null;
    }

    /** kafka_dlt-original-offset es un long binario (8 bytes). */
    private static String largo(ConsumerRecord<?, ?> r, String nombre) {
        Header h = r.headers().lastHeader(nombre);
        return h != null && h.value().length == Long.BYTES ? String.valueOf(ByteBuffer.wrap(h.value()).getLong()) : null;
    }

    private static String primeraLinea(String s) {
        if (s == null) {
            return null;
        }
        int i = s.indexOf('\n');
        return i > 0 ? s.substring(0, i) : s;
    }
}
