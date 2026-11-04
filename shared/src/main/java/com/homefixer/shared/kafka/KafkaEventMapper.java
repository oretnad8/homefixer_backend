package com.homefixer.shared.kafka;

import com.homefixer.shared.dto.NotificacionEventDTO;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Traduce el evento que ya se publica en RabbitMQ ({@link NotificacionEventDTO}) al evento del stream Kafka,
 * para publicar el mismo hecho de negocio en ambos brokers sin duplicar la lógica de los servicios.
 */
public final class KafkaEventMapper {

    private KafkaEventMapper() {
    }

    /** Vacío si el routing key no forma parte del stream analítico (chat, certificaciones). */
    public static Optional<SolicitudEventoKafka> desde(String routingKey, NotificacionEventDTO dto, String origen) {
        if (dto == null || dto.getSolicitudId() == null) {
            return Optional.empty();
        }
        return TipoEventoKafka.desdeRoutingKey(routingKey).map(tipo -> new SolicitudEventoKafka(
                dto.getEventoId() != null ? dto.getEventoId() : UUID.randomUUID().toString(),
                tipo,
                SolicitudEventoKafka.VERSION_ACTUAL,
                dto.getFechaEvento() != null ? dto.getFechaEvento().atZone(ZoneId.systemDefault()).toInstant() : Instant.now(),
                origen,
                dto.getSolicitudId(),
                dto.getClienteId(),
                tecnicoUnico(dto.getTecnicosIds()),
                dto.getCategoria(),
                null,
                estadoNuevo(tipo, dto.getEstado()),
                dto.getMonto(),
                dto.getCalificacion()));
    }

    /** pago.procesado no trae estado: el pago deja la solicitud en PAGADA. */
    private static String estadoNuevo(TipoEventoKafka tipo, String estado) {
        return estado == null && tipo == TipoEventoKafka.PAGO_PROCESADO ? "PAGADA" : estado;
    }

    /** En SOLICITUD_CREADA la lista son los técnicos cercanos notificados; solo hay técnico asignado si es uno. */
    private static Long tecnicoUnico(List<Long> tecnicosIds) {
        return tecnicosIds != null && tecnicosIds.size() == 1 ? tecnicosIds.get(0) : null;
    }
}
