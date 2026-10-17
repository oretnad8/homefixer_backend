package com.homefixer.pagos.listener;

import com.homefixer.pagos.service.PagoService;
import com.homefixer.shared.dto.NotificacionEventDTO;
import com.homefixer.shared.messaging.MessagingConstants;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Suscriptor asíncrono: al completarse una solicitud genera su cobro pendiente. ACK/NACK manual. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CobroSolicitudListener {

    private final PagoService pagoService;

    @RabbitListener(queues = MessagingConstants.PAGOS_QUEUE)
    public void onSolicitudActualizada(@Payload NotificacionEventDTO evento,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                       Channel channel) throws IOException {
        try {
            if ("COMPLETADA".equals(evento.getEstado())) {
                Long tecnicoId = evento.getTecnicosIds() != null && !evento.getTecnicosIds().isEmpty()
                        ? evento.getTecnicosIds().get(0) : null;
                pagoService.generarCobro(evento.getSolicitudId(), evento.getClienteId(), tecnicoId,
                        evento.getMonto(), evento.getCategoria());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
            log.error("[NACK] Evento {} enviado a la DLQ {}: {}", evento.getEventoId(), MessagingConstants.PAGOS_DLQ, e.getMessage());
        }
    }
}
