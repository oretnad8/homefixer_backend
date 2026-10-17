package com.homefixer.ubicacion.dto;

import java.time.Instant;
import java.util.List;

/** Técnico en línea con su posición real, la distancia al punto buscado y sus insignias de maestría. */
public record TecnicoCercanoDTO(
        Long id,
        String nombre,
        String telefono,
        String especialidad,
        Double calificacionPromedio,
        Double precioBase,
        String nivelReputacion,
        boolean validado,
        double latitud,
        double longitud,
        double distanciaKm,
        Instant actualizadoEn,
        /** Oficios en los que tiene insignia de maestría (plomeria, electricidad, …). */
        List<String> insignias) {
}
