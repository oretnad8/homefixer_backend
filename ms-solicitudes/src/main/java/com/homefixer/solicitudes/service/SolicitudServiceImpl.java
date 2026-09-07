package com.homefixer.solicitudes.service;

import com.homefixer.solicitudes.client.UbicacionFeignClient;
import com.homefixer.solicitudes.entity.Solicitud;
import com.homefixer.shared.enums.EstadoSolicitud;
import com.homefixer.solicitudes.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final UbicacionFeignClient ubicacionFeignClient;

    @Override
    public Solicitud createSolicitud(Solicitud solicitud) {
        solicitud.setEstadoSolicitud(EstadoSolicitud.CREADA);
        solicitud.setFecha(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }

    @Override
    public Solicitud getSolicitudById(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
    }

    @Override
    public List<Solicitud> getSolicitudesByCliente(Long clienteId) {
        return solicitudRepository.findByClienteId(clienteId);
    }

    @Override
    public List<Solicitud> getSolicitudesByTecnico(Long tecnicoId) {
        return solicitudRepository.findByTecnicoId(tecnicoId);
    }

    @Override
    public Solicitud assignTecnico(Long solicitudId, Long tecnicoId) {
        Solicitud solicitud = getSolicitudById(solicitudId);
        solicitud.setTecnicoId(tecnicoId);
        solicitud.setEstadoSolicitud(EstadoSolicitud.EN_PROCESO);
        return solicitudRepository.save(solicitud);
    }

    @Override
    public Solicitud updateEstado(Long solicitudId, EstadoSolicitud estado) {
        Solicitud solicitud = getSolicitudById(solicitudId);
        solicitud.setEstadoSolicitud(estado);
        return solicitudRepository.save(solicitud);
    }

    @Override
    public List<Long> findNearbyTecnicos(Long solicitudId) {
        Solicitud solicitud = getSolicitudById(solicitudId);
        try {
            return ubicacionFeignClient.buscarTecnicosCercanos(solicitud.getCoordenadas());
        } catch (Exception e) {
            throw new RuntimeException("Error al comunicarse con ms-ubicacion: " + e.getMessage());
        }
    }
}
