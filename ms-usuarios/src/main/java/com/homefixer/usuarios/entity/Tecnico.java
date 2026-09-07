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
}
