package com.homefixer.ubicacion.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Técnico tal como lo devuelve ms-usuarios (solo los campos que se usan aquí). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TecnicoInfo(
        Long id,
        String nombre,
        String telefono,
        String especialidad,
        Double calificacionPromedio,
        Double precioBase,
        String nivelReputacion,
        Boolean validado,
        Boolean disponible) {
}
