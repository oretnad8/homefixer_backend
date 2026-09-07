package com.homefixer.maestria.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String institucion;

    private LocalDate fechaEmision;
    
    private String urlDocumento;
    
    private Boolean verificado;
}
