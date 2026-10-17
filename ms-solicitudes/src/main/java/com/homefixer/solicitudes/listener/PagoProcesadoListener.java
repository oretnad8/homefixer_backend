package com.homefixer.solicitudes.listener;

import com.homefixer.shared.dto.NotificacionEventDTO;
import com.homefixer.shared.messaging.MessagingConstants;
import com.homefixer.solicitudes.service.SolicitudService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Suscriptor asíncrono de pago.procesado: la solicitud pasa de COMPLETADA a PAGADA y desde
 * ahí el técnico y el cliente pueden valorarse. ACK/NACK manual (las colas RPC siguen en AUTO).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PagoProcesadoListener {

    private final SolicitudService solicitudService;

    @RabbitListener(queues = MessagingConstants.SOLICITUDES_PAGOS_QUEUE, ackMode = "MANUAL")
    public void onPagoProcesado(@Payload NotificacionEventDTO evento,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                Channel channel) throws IOException {
        try {
            solicitudService.marcarPagada(evento.getSolicitudId());
            channel.basicAck(deliveryTag, false);
            log.info("[ACK] Solicitud {} marcada como PAGADA (evento {})", evento.getSolicitudId(), evento.getEventoId());
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
            log.error("[NACK] Evento {} enviado a la DLQ {}: {}", evento.getEventoId(),
                    MessagingConstants.SOLICITUDES_PAGOS_DLQ, e.getMessage());
        }
    }
}
