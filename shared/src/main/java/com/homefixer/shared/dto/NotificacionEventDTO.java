package com.homefixer.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionEventDTO implements Serializable {
    private Long solicitudId;
    private Long clienteId;
    private List<Long> tecnicosIds;
    private String tipoServicio;
    private String mensaje;
}
