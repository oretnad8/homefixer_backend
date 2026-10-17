package com.homefixer.pagos.entity;

import com.homefixer.shared.enums.EstadoPago;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long solicitudId;

    @Column(nullable = false)
    private Double monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estadoPago;

    /** tarjeta, efectivo, transferencia, paypal ("pendiente" mientras no se paga) */
    @Column(nullable = false)
    private String metodoPago;

    private String comprobanteUrl;

    // Columnas nullable: los pagos antiguos no las tienen
    private Long clienteId;
    private Long tecnicoId;
    private String descripcion;
    private LocalDateTime fecha;
}
