package com.homefixer.solicitudes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Trazabilidad de la solicitud: un registro por hito con su fecha y hora
 * (CREADA, ACEPTADA, LLEGADA, EN_PROCESO, COMPLETADA, PAGADA, CANCELADA).
 * LLEGADA no es un estado: la marca el técnico al llegar al domicilio.
 */
@Entity
@Table(name = "solicitud_hitos", indexes = @Index(columnList = "solicitudId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudHito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long solicitudId;

    @Column(nullable = false)
    private String hito;

    @Column(nullable = false)
    private LocalDateTime fecha;

    private String detalle;
}
