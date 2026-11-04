package com.homefixer.shared.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Evento de negocio del ciclo de vida de una solicitud, publicado en {@link KafkaTopics#SOLICITUDES_EVENTOS}.
 * Key Kafka = solicitudId: todos los eventos de una solicitud caen en la misma partición y se consumen en orden.
 *
 * @param eventoId       id único; el consumidor descarta duplicados (idempotencia ante reintentos)
 * @param tipo           hecho de negocio ocurrido
 * @param version        versión del esquema, para evolucionarlo sin romper a los consumidores
 * @param ocurridoEn     momento del hecho (no del procesamiento): base de las ventanas de métricas
 * @param origen         microservicio productor (trazabilidad)
 * @param solicitudId    solicitud afectada (key del mensaje)
 * @param clienteId      cliente dueño de la solicitud
 * @param tecnicoId      técnico asignado (null si aún no hay)
 * @param categoria      categoría del servicio: métricas por categoría
 * @param estadoAnterior estado previo, si el productor lo conoce: transiciones entre estados
 * @param estadoNuevo    estado tras el evento: solicitudes por estado
 * @param monto          precio acordado o monto pagado: ingresos en tiempo real
 * @param calificacion   calificación promedio del técnico (VALORACION_REGISTRADA)
 */
public record SolicitudEventoKafka(
        String eventoId,
        TipoEventoKafka tipo,
        Integer version,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant ocurridoEn,
        String origen,
        Long solicitudId,
        Long clienteId,
        Long tecnicoId,
        String categoria,
        String estadoAnterior,
        String estadoNuevo,
        Double monto,
        Double calificacion) {

    public static final int VERSION_ACTUAL = 1;

    /** Valida el contrato mínimo del evento; se llama en el consumidor antes de procesar. */
    public SolicitudEventoKafka validar() {
        if (eventoId == null || eventoId.isBlank()) {
            throw new EventoInvalidoException("eventoId es obligatorio");
        }
        if (tipo == null) {
            throw new EventoInvalidoException("tipo es obligatorio (evento " + eventoId + ")");
        }
        if (solicitudId == null) {
            throw new EventoInvalidoException("solicitudId es obligatorio (evento " + eventoId + ")");
        }
        if (ocurridoEn == null) {
            throw new EventoInvalidoException("ocurridoEn es obligatorio (evento " + eventoId + ")");
        }
        if (monto != null && monto < 0) {
            throw new EventoInvalidoException("monto no puede ser negativo (evento " + eventoId + ")");
        }
        return this;
    }

    public String key() {
        return String.valueOf(solicitudId);
    }
}
