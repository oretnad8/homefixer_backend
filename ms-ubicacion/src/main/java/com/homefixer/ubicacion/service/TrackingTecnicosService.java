package com.homefixer.ubicacion.service;

import com.homefixer.shared.kafka.UbicacionTecnicoEvento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Seguimiento de técnicos a partir del stream GPS (lógica de dominio, sin dependencias de Kafka):
 * última posición, distancia recorrida, velocidad estimada y detección de técnicos que dejaron de
 * reportar sin desconectarse (posible pérdida de señal o app cerrada).
 */
@Slf4j
@Service
public class TrackingTecnicosService {

    private static final double RADIO_TIERRA_KM = 6371.0;

    public record EstadoTecnico(Long tecnicoId, Double latitud, Double longitud, boolean enLinea, boolean ocupado,
                                Long solicitudEnCursoId, Instant ultimoReporte, long reportes,
                                double kmRecorridos, Double velocidadKmH, boolean inactivo, String origen) {
    }

    private static final class Seguimiento {
        UbicacionTecnicoEvento ultimo;
        long reportes;
        double km;
        Double velocidad;
        boolean alertado;
    }

    private final Map<Long, Seguimiento> tecnicos = new ConcurrentHashMap<>();
    private final Duration umbralInactividad;

    public TrackingTecnicosService(@Value("${ubicacion.ttl-segundos:120}") long ttlSegundos) {
        this.umbralInactividad = Duration.ofSeconds(ttlSegundos);
    }

    public void registrar(UbicacionTecnicoEvento e) {
        Seguimiento s = tecnicos.computeIfAbsent(e.tecnicoId(), k -> new Seguimiento());
        synchronized (s) {
            UbicacionTecnicoEvento previo = s.ultimo;
            // Eventos fuera de orden (p. ej. replay) no retroceden la posición
            if (previo != null && e.registradoEn().isBefore(previo.registradoEn())) {
                return;
            }
            if (previo != null && Boolean.TRUE.equals(previo.enLinea()) && Boolean.TRUE.equals(e.enLinea())) {
                double km = distanciaKm(previo.latitud(), previo.longitud(), e.latitud(), e.longitud());
                double horas = Duration.between(previo.registradoEn(), e.registradoEn()).toMillis() / 3_600_000.0;
                s.km += km;
                s.velocidad = horas > 0 ? Math.round(km / horas * 10.0) / 10.0 : s.velocidad;
            }
            if (s.alertado && Boolean.TRUE.equals(e.enLinea())) {
                log.info("[TRACKING] Técnico {} volvió a reportar posición", e.tecnicoId());
            }
            s.ultimo = e;
            s.reportes++;
            s.alertado = false;
        }
    }

    /** Técnicos en línea sin reportes dentro del umbral: se alerta una sola vez hasta que vuelvan a reportar. */
    public List<Long> detectarInactivos(Instant ahora) {
        return tecnicos.entrySet().stream()
                .filter(en -> {
                    Seguimiento s = en.getValue();
                    synchronized (s) {
                        boolean inactivo = esInactivo(s, ahora);
                        if (inactivo && !s.alertado) {
                            s.alertado = true;
                            return true;
                        }
                        return false;
                    }
                })
                .map(Map.Entry::getKey)
                .toList();
    }

    public List<EstadoTecnico> estados() {
        Instant ahora = Instant.now();
        return tecnicos.entrySet().stream()
                .map(en -> estado(en.getKey(), en.getValue(), ahora))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(EstadoTecnico::tecnicoId))
                .toList();
    }

    public EstadoTecnico estado(Long tecnicoId) {
        Seguimiento s = tecnicos.get(tecnicoId);
        return s == null ? null : estado(tecnicoId, s, Instant.now());
    }

    private EstadoTecnico estado(Long id, Seguimiento s, Instant ahora) {
        synchronized (s) {
            UbicacionTecnicoEvento u = s.ultimo;
            if (u == null) {
                return null; // recién creado, aún sin posición registrada
            }
            return new EstadoTecnico(id, u.latitud(), u.longitud(), Boolean.TRUE.equals(u.enLinea()),
                    u.solicitudEnCursoId() != null, u.solicitudEnCursoId(), u.registradoEn(), s.reportes,
                    Math.round(s.km * 100.0) / 100.0, s.velocidad, esInactivo(s, ahora), u.origen());
        }
    }

    private boolean esInactivo(Seguimiento s, Instant ahora) {
        return s.ultimo != null && Boolean.TRUE.equals(s.ultimo.enLinea())
                && s.ultimo.registradoEn().isBefore(ahora.minus(umbralInactividad));
    }

    static double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
