package com.homefixer.ubicacion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Última posición GPS reportada por un técnico (una fila por técnico). */
@Entity
@Table(name = "ubicaciones_tecnicos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionTecnico {

    @Id
    private Long tecnicoId;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    /** Precisión del GPS en metros, según el navegador. */
    private Double precisionMetros;

    @Column(nullable = false)
    private Instant actualizadoEn;
}
