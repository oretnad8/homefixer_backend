package com.homefixer.solicitudes.entity;

import com.homefixer.shared.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Solicitud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clienteId;

    private Long tecnicoId;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitud estadoSolicitud;

    private Double precioEstimado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private String coordenadas; // e.g. "lat,lon"

    // ---- Datos del formulario del cliente (columnas nullable: filas antiguas no los tienen) ----

    /** plomeria, electricidad, carpinteria, pintura, cerrajeria, limpieza, jardineria, refrigeracion, otros */
    private String categoria;

    /** normal, urgente, emergencia */
    private String urgencia;

    /** Dirección escrita por el cliente (coordenadas guarda la geocodificación). */
    private String direccion;

    private String clienteNombre;

    // ---- Copia de los datos del técnico al momento de la asignación ----

    private String tecnicoNombre;
    private String tecnicoTelefono;
    private String tecnicoEspecialidad;
    private Double tecnicoCalificacion;

    @Column(length = 1000)
    private String notasCierre;

    private LocalDateTime fechaActualizacion;
}
