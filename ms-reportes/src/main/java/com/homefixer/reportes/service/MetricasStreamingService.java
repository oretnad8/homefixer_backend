package com.homefixer.reportes.service;

import com.homefixer.reportes.dto.MetricasTiempoRealDTO;
import com.homefixer.reportes.dto.MetricasTiempoRealDTO.EventoReciente;
import com.homefixer.reportes.dto.MetricasTiempoRealDTO.TecnicoPosicion;
import com.homefixer.shared.kafka.SolicitudEventoKafka;
import com.homefixer.shared.kafka.TipoEventoKafka;
import com.homefixer.shared.kafka.UbicacionTecnicoEvento;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Lógica de dominio de las métricas en tiempo real. No depende de Kafka: los listeners le entregan
 * eventos ya deserializados y validados. Estado en memoria; al reiniciar, el consumer group se
 * reconstruye desde el log retenido (auto-offset-reset=earliest con un grupo nuevo, o reset de offsets).
 */
@Service
public class MetricasStreamingService {

    private static final Duration VENTANA = Duration.ofMinutes(1);
    private static final Duration TECNICO_EN_LINEA = Duration.ofMinutes(2);
    private static final int MAX_IDS_PROCESADOS = 10_000;
    private static final int MAX_EVENTOS_RECIENTES = 20;

    /** Idempotencia: ids ya procesados (LRU acotado), un reintento o un re-envío no duplica métricas. */
    private final Set<String> procesados = Collections.newSetFromMap(new LinkedHashMap<>(256, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_IDS_PROCESADOS;
        }
    });

    private final AtomicLong totalEventos = new AtomicLong();
    private final Map<String, AtomicLong> eventosPorTipo = new ConcurrentHashMap<>();
    /** Último estado conocido de cada solicitud (la key Kafka garantiza el orden por solicitud). */
    private final Map<Long, String> estadoPorSolicitud = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> solicitudesPorCategoria = new ConcurrentHashMap<>();
    private final DoubleAdder ingresos = new DoubleAdder();
    private final DoubleAdder sumaCalificaciones = new DoubleAdder();
    private final AtomicLong cantidadCalificaciones = new AtomicLong();
    private final Map<Long, UbicacionTecnicoEvento> ultimaUbicacion = new ConcurrentHashMap<>();

    private final Deque<Instant> ventanaEventos = new ArrayDeque<>();
    private final Deque<Instant> ventanaGps = new ArrayDeque<>();
    private final Deque<EventoReciente> recientes = new ArrayDeque<>();

    /** Cambia con cada evento: el SSE solo empuja cuando hay datos nuevos. */
    private final AtomicLong version = new AtomicLong();

    /** @return false si el evento ya se había procesado (duplicado). */
    public boolean registrarEvento(SolicitudEventoKafka e) {
        synchronized (procesados) {
            if (!procesados.add(e.eventoId())) {
                return false;
            }
        }
        totalEventos.incrementAndGet();
        eventosPorTipo.computeIfAbsent(e.tipo().name(), k -> new AtomicLong()).incrementAndGet();
        if (e.estadoNuevo() != null) {
            estadoPorSolicitud.put(e.solicitudId(), e.estadoNuevo());
        }
        if (e.tipo() == TipoEventoKafka.SOLICITUD_CREADA) {
            String categoria = e.categoria() != null ? e.categoria() : "otros";
            solicitudesPorCategoria.computeIfAbsent(categoria, k -> new AtomicLong()).incrementAndGet();
        }
        if (e.tipo() == TipoEventoKafka.PAGO_PROCESADO && e.monto() != null) {
            ingresos.add(e.monto());
        }
        if (e.tipo() == TipoEventoKafka.VALORACION_REGISTRADA && e.calificacion() != null) {
            sumaCalificaciones.add(e.calificacion());
            cantidadCalificaciones.incrementAndGet();
        }
        Instant ahora = Instant.now();
        synchronized (ventanaEventos) {
            ventanaEventos.addLast(ahora);
            recientes.addFirst(new EventoReciente(e.eventoId(), e.tipo().name(), e.solicitudId(), e.estadoNuevo(),
                    e.categoria(), e.monto(), e.origen(), e.ocurridoEn()));
            while (recientes.size() > MAX_EVENTOS_RECIENTES) {
                recientes.removeLast();
            }
        }
        version.incrementAndGet();
        return true;
    }

    public void registrarUbicacion(UbicacionTecnicoEvento e) {
        if (Boolean.TRUE.equals(e.enLinea())) {
            ultimaUbicacion.put(e.tecnicoId(), e);
        } else {
            ultimaUbicacion.remove(e.tecnicoId());
        }
        synchronized (ventanaGps) {
            ventanaGps.addLast(Instant.now());
        }
        version.incrementAndGet();
    }

    public long version() {
        return version.get();
    }

    public MetricasTiempoRealDTO snapshot() {
        Instant ahora = Instant.now();
        Instant limiteVentana = ahora.minus(VENTANA);
        long eventosMinuto;
        List<EventoReciente> ultimos;
        synchronized (ventanaEventos) {
            podar(ventanaEventos, limiteVentana);
            eventosMinuto = ventanaEventos.size();
            ultimos = new ArrayList<>(recientes);
        }
        long gpsMinuto;
        synchronized (ventanaGps) {
            podar(ventanaGps, limiteVentana);
            gpsMinuto = ventanaGps.size();
        }

        Instant limiteEnLinea = ahora.minus(TECNICO_EN_LINEA);
        List<TecnicoPosicion> tecnicos = ultimaUbicacion.values().stream()
                .filter(u -> u.registradoEn().isAfter(limiteEnLinea))
                .sorted(Comparator.comparing(UbicacionTecnicoEvento::tecnicoId))
                .map(u -> new TecnicoPosicion(u.tecnicoId(), u.latitud(), u.longitud(),
                        u.solicitudEnCursoId() != null, u.registradoEn(), u.origen()))
                .toList();

        Map<String, Long> porEstado = new TreeMap<>();
        estadoPorSolicitud.values().forEach(estado -> porEstado.merge(estado, 1L, Long::sum));

        long nCalif = cantidadCalificaciones.get();
        return new MetricasTiempoRealDTO(
                ahora,
                totalEventos.get(),
                eventosMinuto,
                copiar(eventosPorTipo),
                porEstado,
                copiar(solicitudesPorCategoria),
                ingresos.sum(),
                nCalif == 0 ? null : Math.round(sumaCalificaciones.sum() / nCalif * 100.0) / 100.0,
                tecnicos.size(),
                tecnicos.stream().filter(TecnicoPosicion::ocupado).count(),
                gpsMinuto,
                tecnicos,
                ultimos);
    }

    private static void podar(Deque<Instant> ventana, Instant limite) {
        while (!ventana.isEmpty() && ventana.peekFirst().isBefore(limite)) {
            ventana.removeFirst();
        }
    }

    private static Map<String, Long> copiar(Map<String, AtomicLong> origen) {
        Map<String, Long> copia = new TreeMap<>();
        origen.forEach((k, v) -> copia.put(k, v.get()));
        return copia;
    }
}
