package com.homefixer.solicitudes.service;

import com.homefixer.solicitudes.entity.Solicitud;
import com.homefixer.shared.enums.EstadoSolicitud;

import java.util.List;

public interface SolicitudService {
    Solicitud createSolicitud(Solicitud solicitud);
    Solicitud getSolicitudById(Long id);
    List<Solicitud> getSolicitudesByCliente(Long clienteId);
    List<Solicitud> getSolicitudesByTecnico(Long tecnicoId);
    Solicitud assignTecnico(Long solicitudId, Long tecnicoId);
    Solicitud updateEstado(Long solicitudId, EstadoSolicitud estado);
    List<Long> findNearbyTecnicos(Long solicitudId);
}
