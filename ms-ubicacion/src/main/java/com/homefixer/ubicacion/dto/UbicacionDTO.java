package com.homefixer.ubicacion.dto;

import java.time.Instant;

/** Posición de un técnico; enLinea = reportó dentro de la ventana de vigencia. */
public record UbicacionDTO(
        Long tecnicoId,
        double latitud,
        double longitud,
        Double precisionMetros,
        Instant actualizadoEn,
        boolean enLinea) {
}
