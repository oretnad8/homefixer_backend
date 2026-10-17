package com.homefixer.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/** Respuesta de la consulta síncrona (RPC sobre RabbitMQ) de ms-reportes a ms-solicitudes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenSolicitudesDTO implements Serializable {
    private long totalSolicitudes;
    private Map<String, Long> solicitudesPorEstado;
    private LocalDateTime generadoEn;
}
