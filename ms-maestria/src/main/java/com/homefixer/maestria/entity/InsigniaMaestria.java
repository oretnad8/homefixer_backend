package com.homefixer.maestria.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Insignia "Maestro en <oficio>": se otorga al aprobarse una certificación (una por oficio). */
@Entity
@Table(name = "insignias_maestria")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsigniaMaestria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tecnicoId;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    /** Identificador del emblema que dibuja el front ("categoria:electricidad"). */
    @Column(nullable = false)
    private String iconoUrl;

    private String categoria;

    /** Certificación aprobada que la originó. */
    private Long certificacionId;

    private LocalDateTime fechaOtorgada;
}
