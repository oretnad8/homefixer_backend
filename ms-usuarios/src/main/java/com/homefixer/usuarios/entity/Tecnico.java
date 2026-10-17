package com.homefixer.usuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tecnicos")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Tecnico extends Usuario {

    @Column(nullable = false)
    private Double calificacionPromedio = 0.0;

    @Column(nullable = false)
    private Boolean validado = false;

    @Column(nullable = false)
    private String nivelReputacion = "NUEVO";

    @Column(nullable = false)
    private String especialidad = "otros"; // plomeria, electricidad, etc.

    @Column(nullable = false)
    private Double precioBase = 25000.0;

    /** Interruptor "Disponible" del técnico; null (filas antiguas) se considera disponible. */
    private Boolean disponible = true;
}
