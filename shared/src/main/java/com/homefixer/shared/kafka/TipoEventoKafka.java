package com.homefixer.shared.kafka;

import com.homefixer.shared.messaging.MessagingConstants;

import java.util.Arrays;
import java.util.Optional;

/** Tipos de evento de negocio que viajan por homefixer.solicitudes.eventos. */
public enum TipoEventoKafka {
    SOLICITUD_CREADA(MessagingConstants.RK_SOLICITUD_CREADA),
    TECNICO_ASIGNADO(MessagingConstants.RK_SOLICITUD_TECNICO_ASIGNADO),
    ESTADO_ACTUALIZADO(MessagingConstants.RK_SOLICITUD_ESTADO_ACTUALIZADO),
    PAGO_PROCESADO(MessagingConstants.RK_PAGO_PROCESADO),
    VALORACION_REGISTRADA(MessagingConstants.RK_VALORACION_REGISTRADA);

    /** Routing key RabbitMQ equivalente (el mismo hecho de negocio publicado en ambos brokers). */
    private final String routingKey;

    TipoEventoKafka(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    /** Eventos de RabbitMQ que no son parte del stream analítico (chat, certificaciones) no tienen tipo. */
    public static Optional<TipoEventoKafka> desdeRoutingKey(String routingKey) {
        return Arrays.stream(values()).filter(t -> t.routingKey.equals(routingKey)).findFirst();
    }
}
