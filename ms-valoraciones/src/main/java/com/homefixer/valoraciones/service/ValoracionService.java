package com.homefixer.valoraciones.service;

import com.homefixer.shared.dto.NotificacionEventDTO;
import com.homefixer.shared.dto.SolicitudDetalleDTO;
import com.homefixer.shared.messaging.EventPublisher;
import com.homefixer.shared.messaging.MessagingConstants;
import com.homefixer.valoraciones.entity.Valoracion;
import com.homefixer.valoraciones.repository.ValoracionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValoracionService {

    private final ValoracionRepository valoracionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final EventPublisher eventPublisher;

    /**
     * Registra la valoración de una solicitud ya pagada (PAGADA). Los datos de la solicitud se
     * obtienen por RPC (síncrono) y el resultado se publica como evento (asíncrono):
     * ms-usuarios actualiza la calificación y ms-notificacion avisa al valorado.
     */
    @Transactional
    public Valoracion registrar(Long solicitudId, Integer calificacion, String comentario, String autorRol) {
        String autor = "tecnico".equals(autorRol) ? "tecnico" : "cliente";
        if (calificacion == null || calificacion < 1 || calificacion > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La calificación debe estar entre 1 y 5.");
        }
        SolicitudDetalleDTO solicitud = obtenerSolicitud(solicitudId);
        if (solicitud.getTecnicoId() == null
                || !("COMPLETADA".equals(solicitud.getEstado()) || "PAGADA".equals(solicitud.getEstado()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede valorar un servicio completado.");
        }
        // Flujo de finalización: pago solicitado -> pago procesado -> valoración
        if (!"PAGADA".equals(solicitud.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El servicio aún no ha sido pagado. Podrás valorar cuando se reciba el pago.");
        }
        if (valoracionRepository.yaValorada(solicitudId, autor)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya valoraste este servicio.");
        }

        Valoracion guardada = valoracionRepository.save(Valoracion.builder()
                .solicitudId(solicitudId)
                .tecnicoId(solicitud.getTecnicoId())
                .clienteId(solicitud.getClienteId())
                .puntuacion(calificacion)
                .comentario(comentario)
                .autorRol(autor)
                .fecha(LocalDateTime.now())
                .build());

        boolean alTecnico = "cliente".equals(autor);
        List<Valoracion> delTecnico = alTecnico ? valoracionRepository.findDeClientesAlTecnico(solicitud.getTecnicoId()) : List.of();
        Double promedio = alTecnico ? delTecnico.stream().mapToInt(Valoracion::getPuntuacion).average().orElse(0.0) : null;
        Long valorado = alTecnico ? solicitud.getTecnicoId() : solicitud.getClienteId();
        eventPublisher.publicar(MessagingConstants.RK_VALORACION_REGISTRADA, NotificacionEventDTO.builder()
                .solicitudId(solicitudId)
                .clienteId(solicitud.getClienteId())
                .tecnicosIds(List.of(solicitud.getTecnicoId()))
                .destinatariosIds(List.of(valorado))
                .tipoServicio(alTecnico ? "VALORACION_TECNICO" : "VALORACION_CLIENTE")
                .calificacion(promedio)
                .totalValoraciones(alTecnico ? delTecnico.size() : null)
                .mensaje("Recibiste una valoración de " + calificacion + " estrella(s) por la solicitud #" + solicitudId + ".")
                .build());
        return guardada;
    }

    public List<Valoracion> getValoracionesByTecnico(Long tecnicoId) {
        return valoracionRepository.findDeClientesAlTecnico(tecnicoId);
    }

    public List<Valoracion> getValoracionesBySolicitud(Long solicitudId) {
        return valoracionRepository.findBySolicitudId(solicitudId);
    }

    /** RPC request/reply sobre RabbitMQ a ms-solicitudes. */
    private SolicitudDetalleDTO obtenerSolicitud(Long solicitudId) {
        SolicitudDetalleDTO detalle = rabbitTemplate.convertSendAndReceiveAsType(
                MessagingConstants.RPC_EXCHANGE, MessagingConstants.RK_RPC_DETALLE_SOLICITUD, solicitudId,
                new ParameterizedTypeReference<SolicitudDetalleDTO>() {});
        if (detalle == null) {
            log.error("[ALERTA][RPC] ms-solicitudes no respondió el detalle de la solicitud {}", solicitudId);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el servicio. Intenta más tarde.");
        }
        if (!detalle.isEncontrada()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada.");
        }
        return detalle;
    }
}
