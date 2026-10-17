package com.homefixer.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/** Evento de dominio publicado en homefixer.events (solicitudes, pagos, valoraciones, chat, certificaciones). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionEventDTO implements Serializable {
    /** Identificador único del evento (correlación de confirms y trazabilidad en DLQ). */
    private String eventoId;
    private LocalDateTime fechaEvento;
    private Long solicitudId;
    private Long clienteId;
    private List<Long> tecnicosIds;
    /** Usuarios que deben recibir la notificación de este evento. */
    private List<Long> destinatariosIds;
    private String tipoServicio;
    /** Estado de la solicitud tras el evento (CREADA, ACEPTADA, EN_PROCESO, COMPLETADA, PAGADA, CANCELADA). */
    private String estado;
    /** Categoría de la solicitud (plomeria, electricidad, …). */
    private String categoria;
    /** Monto del servicio (precio acordado o monto pagado). */
    private Double monto;
    /** Nueva calificación promedio del técnico (eventos de valoración). */
    private Double calificacion;
    /** Cantidad de valoraciones de clientes del técnico (eventos de valoración); define su nivel. */
    private Integer totalValoraciones;
    private String mensaje;
}
