package com.homefixer.shared.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Posición GPS de un técnico, publicada en {@link KafkaTopics#TECNICOS_UBICACION} (flujo continuo).
 * Key Kafka = tecnicoId: con cleanup.policy=compact el tópico conserva la última posición de cada técnico.
 *
 * @param eventoId           id único del reporte de posición
 * @param tecnicoId          técnico (key del mensaje)
 * @param latitud            -90..90
 * @param longitud           -180..180
 * @param enLinea            false cuando el técnico se desconecta
 * @param solicitudEnCursoId solicitud que atiende (null = libre): técnicos ocupados vs. disponibles
 * @param registradoEn       momento de la lectura GPS
 * @param origen             "ms-ubicacion" o "simulador"
 */
public record UbicacionTecnicoEvento(
        String eventoId,
        Long tecnicoId,
        Double latitud,
        Double longitud,
        Boolean enLinea,
        Long solicitudEnCursoId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant registradoEn,
        String origen) {

    public UbicacionTecnicoEvento validar() {
        if (eventoId == null || eventoId.isBlank()) {
            throw new EventoInvalidoException("eventoId es obligatorio");
        }
        if (tecnicoId == null) {
            throw new EventoInvalidoException("tecnicoId es obligatorio (evento " + eventoId + ")");
        }
        if (registradoEn == null || enLinea == null) {
            throw new EventoInvalidoException("registradoEn y enLinea son obligatorios (evento " + eventoId + ")");
        }
        if (Boolean.TRUE.equals(enLinea)) {
            if (latitud == null || latitud < -90 || latitud > 90) {
                throw new EventoInvalidoException("latitud fuera de rango: " + latitud + " (evento " + eventoId + ")");
            }
            if (longitud == null || longitud < -180 || longitud > 180) {
                throw new EventoInvalidoException("longitud fuera de rango: " + longitud + " (evento " + eventoId + ")");
            }
        }
        return this;
    }

    public String key() {
        return String.valueOf(tecnicoId);
    }
}
