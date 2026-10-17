package com.homefixer.solicitudes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Mensaje del chat entre cliente y técnico de una solicitud. */
@Entity
@Table(name = "mensajes_chat", indexes = @Index(columnList = "solicitudId"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long solicitudId;

    @Column(nullable = false)
    private Long remitenteId;

    @Column(nullable = false)
    private String remitenteNombre;

    @Column(nullable = false)
    private Boolean esCliente;

    @Column(nullable = false, length = 2000)
    private String texto;

    @Column(nullable = false)
    private LocalDateTime creadoEn;
}
