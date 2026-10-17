package com.homefixer.notificacion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Notificación in-app de un usuario, creada a partir de un evento de RabbitMQ. */
@Entity
@Table(name = "notificaciones",
        uniqueConstraints = @UniqueConstraint(columnNames = {"eventoId", "usuarioId"}),
        indexes = @Index(columnList = "usuarioId"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    /** Id del evento de origen: evita duplicados si el mensaje se reprocesa. */
    @Column(nullable = false)
    private String eventoId;

    private Long solicitudId;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String detalle;

    @Column(nullable = false)
    private Boolean leida;

    @Column(nullable = false)
    private LocalDateTime creadoEn;
}
