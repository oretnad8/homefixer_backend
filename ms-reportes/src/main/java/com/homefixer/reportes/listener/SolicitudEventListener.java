package com.homefixer.reportes.listener;

import com.homefixer.reportes.service.MetricasEventosService;
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

/** Consumidor asíncrono de eventos de solicitudes para métricas, con ACK/NACK manual. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitudEventListener {

    private final MetricasEventosService metricasEventosService;

    @RabbitListener(queues = MessagingConstants.REPORTES_QUEUE)
    public void registrarEvento(@Payload NotificacionEventDTO evento,
                                @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                Channel channel) throws IOException {
        try {
            metricasEventosService.registrar(routingKey, evento);
            channel.basicAck(deliveryTag, false);
            log.info("[ACK] Evento {} [{}] registrado en métricas", evento.getEventoId(), routingKey);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
            log.error("[NACK] Evento {} no se pudo registrar, enviado a la DLQ {}: {}",
                    evento.getEventoId(), MessagingConstants.REPORTES_DLQ, e.getMessage());
        }
    }
}
