package com.homefixer.solicitudes.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Un técnico descartó una solicitud disponible: deja de mostrársele. */
@Entity
@Table(name = "solicitud_rechazos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"solicitudId", "tecnicoId"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudRechazo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long solicitudId;

    @Column(nullable = false)
    private Long tecnicoId;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
