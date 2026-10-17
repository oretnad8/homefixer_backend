package com.homefixer.solicitudes.service;

import com.homefixer.solicitudes.entity.Solicitud;
import com.homefixer.shared.enums.EstadoSolicitud;

import java.util.List;
import java.util.Map;

public interface SolicitudService {
    Solicitud createSolicitud(Solicitud solicitud);
    Solicitud getSolicitudById(Long id);
    List<Solicitud> getSolicitudesByCliente(Long clienteId);
    List<Solicitud> getSolicitudesByTecnico(Long tecnicoId);
    List<Solicitud> getSolicitudesDisponibles(Long tecnicoId);
    long countSolicitudesActivasByTecnico(Long tecnicoId);

    /** Cliente elige técnico o técnico acepta una disponible: CREADA -> ACEPTADA. */
    Solicitud assignTecnico(Long solicitudId, Long tecnicoId);
    void rechazar(Long solicitudId, Long tecnicoId);
    /** ACEPTADA -> EN_PROCESO */
    Solicitud iniciar(Long solicitudId);
    /** EN_PROCESO -> COMPLETADA (se solicita el pago al cliente) */
    Solicitud finalizar(Long solicitudId, String notas);
    /** COMPLETADA -> PAGADA, al recibir el evento pago.procesado. Idempotente. */
    void marcarPagada(Long solicitudId);
    /** CREADA | ACEPTADA -> CANCELADA */
    Solicitud cancelar(Long solicitudId);
    /** Cambio de estado administrativo (sin validar la transición). */
    Solicitud updateEstado(Long solicitudId, EstadoSolicitud estado);

    Map<String, Object> estadisticasTecnico(Long tecnicoId);
    List<Long> findNearbyTecnicos(Long solicitudId);
}
