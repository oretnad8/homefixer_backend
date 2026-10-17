package com.homefixer.pagos.client;

import com.homefixer.shared.dto.SolicitudDetalleDTO;
import com.homefixer.shared.messaging.MessagingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Consulta síncrona (RPC request/reply sobre RabbitMQ) del detalle de una solicitud. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitudesRpcClient {

    private final RabbitTemplate rabbitTemplate;

    public SolicitudDetalleDTO obtenerDetalle(Long solicitudId) {
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
