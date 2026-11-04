package com.homefixer.reportes.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Snapshot de las métricas calculadas en tiempo real a partir de los tópicos Kafka. */
public record MetricasTiempoRealDTO(
        Instant generadoEn,
        long totalEventos,
        long eventosUltimoMinuto,
        Map<String, Long> eventosPorTipo,
        Map<String, Long> solicitudesPorEstado,
        Map<String, Long> solicitudesPorCategoria,
        double ingresosTotales,
        Double calificacionPromedio,
        long tecnicosEnLinea,
        long tecnicosOcupados,
        long actualizacionesGpsUltimoMinuto,
        List<TecnicoPosicion> tecnicos,
        List<EventoReciente> ultimosEventos) {

    public record TecnicoPosicion(Long tecnicoId, Double latitud, Double longitud, boolean ocupado,
                                  Instant registradoEn, String origen) {
    }

    public record EventoReciente(String eventoId, String tipo, Long solicitudId, String estado,
                                 String categoria, Double monto, String origen, Instant ocurridoEn) {
    }
}
