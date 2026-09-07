package com.homefixer.valoraciones.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "valoraciones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Valoracion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long solicitudId;

    @Column(nullable = false)
    private Long tecnicoId;

    @Column(nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private Integer puntuacion;

    private String comentario;
}
