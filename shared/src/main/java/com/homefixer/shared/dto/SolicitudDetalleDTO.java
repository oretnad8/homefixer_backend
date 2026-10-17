package com.homefixer.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Respuesta RPC (síncrona sobre RabbitMQ) de ms-solicitudes con los datos de una solicitud.
 * encontrada=false si el id no existe.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDetalleDTO implements Serializable {
    private boolean encontrada;
    private Long id;
    private Long clienteId;
    private Long tecnicoId;
    private String estado;
    private String categoria;
    private String descripcion;
    private Double precioEstimado;
}
