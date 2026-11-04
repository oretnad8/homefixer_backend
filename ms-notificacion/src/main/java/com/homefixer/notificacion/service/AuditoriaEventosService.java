package com.homefixer.notificacion.service;

import com.homefixer.shared.kafka.SolicitudEventoKafka;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Auditoría del ciclo de vida de las solicitudes a partir del stream Kafka (lógica de dominio, sin
 * dependencias de Kafka). Reconstruye el historial de cada solicitud en el orden en que ocurrió: como la
 * key es el solicitudId, todos sus eventos llegan por la misma partición y en orden.
 * Detecta transiciones inválidas (p. ej. PAGADA antes de COMPLETADA) y eventos duplicados.
 */
@Service
public class AuditoriaEventosService {

    private static final int MAX_SOLICITUDES = 5_000;
    private static final int MAX_IDS = 20_000;
    private static final int MAX_ANOMALIAS = 100;

    /** Orden válido de estados; CANCELADA puede llegar desde cualquier estado no final. */
    private static final List<String> ORDEN = List.of("CREADA", "ACEPTADA", "EN_PROCESO", "COMPLETADA", "PAGADA");

    public record EntradaHistorial(String eventoId, String tipo, String estado, String origen, Instant ocurridoEn,
                                   Double monto, Double calificacion) {
    }

    public record Anomalia(Long solicitudId, String eventoId, String detalle, Instant detectadaEn) {
    }

    public record ResumenAuditoria(long eventosAuditados, long duplicadosDescartados, int solicitudesSeguidas,
                                   Map<String, Long> eventosPorOrigen, Map<String, Long> eventosPorTipo,
                                   List<Anomalia> anomalias) {
    }

    private final Map<Long, List<EntradaHistorial>> historial = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, List<EntradaHistorial>> eldest) {
            return size() > MAX_SOLICITUDES;
        }
    };
    private final Set<String> ids = Collections.newSetFromMap(new LinkedHashMap<>(256, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_IDS;
        }
    });
    private final Map<String, Long> porOrigen = new TreeMap<>();
    private final Map<String, Long> porTipo = new TreeMap<>();
    private final List<Anomalia> anomalias = new ArrayList<>();
    private long auditados;
    private long duplicados;

    /** @return la anomalía detectada, o null si el evento es coherente con el historial. */
    public synchronized Anomalia registrar(SolicitudEventoKafka e) {
        if (!ids.add(e.eventoId())) {
            duplicados++;
            return null;
        }
        auditados++;
        porOrigen.merge(e.origen() != null ? e.origen() : "desconocido", 1L, Long::sum);
        porTipo.merge(e.tipo().name(), 1L, Long::sum);

        List<EntradaHistorial> eventos = historial.computeIfAbsent(e.solicitudId(), k -> new ArrayList<>());
        String anterior = ultimoEstado(eventos);
        eventos.add(new EntradaHistorial(e.eventoId(), e.tipo().name(), e.estadoNuevo(), e.origen(), e.ocurridoEn(),
                e.monto(), e.calificacion()));

        String problema = validarTransicion(anterior, e.estadoNuevo());
        if (problema == null) {
            return null;
        }
        Anomalia a = new Anomalia(e.solicitudId(), e.eventoId(), problema, Instant.now());
        anomalias.add(0, a);
        if (anomalias.size() > MAX_ANOMALIAS) {
            anomalias.remove(anomalias.size() - 1);
        }
        return a;
    }

    public synchronized List<EntradaHistorial> historial(Long solicitudId) {
        return List.copyOf(historial.getOrDefault(solicitudId, List.of()));
    }

    public synchronized ResumenAuditoria resumen() {
        return new ResumenAuditoria(auditados, duplicados, historial.size(), new TreeMap<>(porOrigen),
                new TreeMap<>(porTipo), List.copyOf(anomalias));
    }

    /** Los estados solo avanzan (o se cancelan); repetir el mismo estado es válido (p. ej. llegada del técnico). */
    static String validarTransicion(String anterior, String nuevo) {
        if (anterior == null || nuevo == null || anterior.equals(nuevo)) {
            return null;
        }
        if ("CANCELADA".equals(anterior) || "PAGADA".equals(anterior)) {
            return "Evento " + nuevo + " sobre una solicitud ya " + anterior;
        }
        if ("CANCELADA".equals(nuevo)) {
            return null;
        }
        int a = ORDEN.indexOf(anterior);
        int n = ORDEN.indexOf(nuevo);
        if (a >= 0 && n >= 0 && n < a) {
            return "Transición hacia atrás " + anterior + " -> " + nuevo;
        }
        return null;
    }

    private static String ultimoEstado(List<EntradaHistorial> eventos) {
        for (int i = eventos.size() - 1; i >= 0; i--) {
            if (eventos.get(i).estado() != null) {
                return eventos.get(i).estado();
            }
        }
        return null;
    }
}
