package com.homefixer.solicitudes.listener;

import com.homefixer.shared.dto.SolicitudDetalleDTO;
import com.homefixer.shared.messaging.MessagingConstants;
import com.homefixer.solicitudes.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Servidor RPC (síncrono request/reply): ms-pagos y ms-valoraciones consultan aquí los datos
 * de una solicitud. La respuesta va a la cola indicada en reply_to.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DetalleSolicitudRpcListener {

    private final SolicitudRepository solicitudRepository;

    @RabbitListener(queues = MessagingConstants.RPC_DETALLE_SOLICITUD_QUEUE)
    public SolicitudDetalleDTO responderDetalle(Long solicitudId) {
        log.info("[RPC] Petición de detalle de la solicitud {}", solicitudId);
        return solicitudRepository.findById(solicitudId)
                .map(s -> SolicitudDetalleDTO.builder()
                        .encontrada(true)
                        .id(s.getId())
                        .clienteId(s.getClienteId())
                        .tecnicoId(s.getTecnicoId())
                        .estado(s.getEstadoSolicitud().name())
                        .categoria(s.getCategoria())
                        .descripcion(s.getDescripcion())
                        .precioEstimado(s.getPrecioEstimado())
                        .build())
                .orElseGet(() -> SolicitudDetalleDTO.builder().encontrada(false).id(solicitudId).build());
    }
}
