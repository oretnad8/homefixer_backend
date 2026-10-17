package com.homefixer.maestria.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificaciones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tecnicoId;

    /** Nombre del técnico al postular (para la revisión del admin sin otra consulta). */
    private String tecnicoNombre;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String institucion;

    /** Oficio que acredita (plomeria, electricidad, …): define la insignia que otorga. */
    private String categoria;

    private LocalDate fechaEmision;

    /** Archivo subido a Supabase Storage. */
    private String urlDocumento;

    /** Columnas nuevas sin NOT NULL: ddl-auto=update no puede agregarlo con filas existentes. */
    @Enumerated(EnumType.STRING)
    private EstadoCertificacion estado;

    private String motivoRechazo;

    private LocalDateTime fechaSolicitud;

    private LocalDateTime fechaRevision;

    /** Se mantiene por compatibilidad: true solo si está APROBADA. */
    private Boolean verificado;
}
