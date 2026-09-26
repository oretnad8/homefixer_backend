package com.homefixer.solicitudes.service;

import com.homefixer.shared.dto.NotificacionEventDTO;
import com.homefixer.solicitudes.client.UbicacionFeignClient;
import com.homefixer.solicitudes.entity.Solicitud;
import com.homefixer.shared.enums.EstadoSolicitud;
import com.homefixer.solicitudes.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final UbicacionFeignClient ubicacionFeignClient;
    private final EventoPublisherService eventoPublisherService;

    @Override
    public Solicitud createSolicitud(Solicitud solicitud) {
        solicitud.setEstadoSolicitud(EstadoSolicitud.CREADA);
        solicitud.setFecha(LocalDateTime.now());
        Solicitud savedSolicitud = solicitudRepository.save(solicitud);
        
        List<Long> tecnicosIds = new ArrayList<>();
        try {
            tecnicosIds = ubicacionFeignClient.buscarTecnicosCercanos(savedSolicitud.getCoordenadas());
        } catch (Exception e) {
            System.err.println("Error al buscar técnicos cercanos: " + e.getMessage());
        }

        NotificacionEventDTO evento = NotificacionEventDTO.builder()
                .solicitudId(savedSolicitud.getId())
                .clienteId(savedSolicitud.getClienteId())
                .tecnicosIds(tecnicosIds)
                .tipoServicio("NUEVA_SOLICITUD")
                .mensaje(savedSolicitud.getDescripcion())
                .build();
                
        eventoPublisherService.publicarNotificacion(evento);
        
        return savedSolicitud;
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
        Solicitud savedSolicitud = solicitudRepository.save(solicitud);
        
        List<Long> tecnicosList = new ArrayList<>();
        tecnicosList.add(tecnicoId);
        
        NotificacionEventDTO evento = NotificacionEventDTO.builder()
                .solicitudId(savedSolicitud.getId())
                .clienteId(savedSolicitud.getClienteId())
                .tecnicosIds(tecnicosList)
                .tipoServicio("TECNICO_ASIGNADO")
                .mensaje("Un técnico ha sido asignado a tu solicitud y está en camino.")
                .build();
                
        eventoPublisherService.publicarNotificacion(evento);
        
        return savedSolicitud;
    }

    @Override
    public Solicitud updateEstado(Long solicitudId, EstadoSolicitud estado) {
        Solicitud solicitud = getSolicitudById(solicitudId);
        solicitud.setEstadoSolicitud(estado);
        Solicitud savedSolicitud = solicitudRepository.save(solicitud);
        
        List<Long> tecnicosList = new ArrayList<>();
        if (savedSolicitud.getTecnicoId() != null) {
            tecnicosList.add(savedSolicitud.getTecnicoId());
        }
        
        NotificacionEventDTO evento = NotificacionEventDTO.builder()
                .solicitudId(savedSolicitud.getId())
                .clienteId(savedSolicitud.getClienteId())
                .tecnicosIds(tecnicosList)
                .tipoServicio("CAMBIO_ESTADO")
                .mensaje("El estado de tu solicitud ha cambiado a: " + estado.name())
                .build();
                
        eventoPublisherService.publicarNotificacion(evento);
        
        return savedSolicitud;
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
