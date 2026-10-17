package com.homefixer.notificacion.listener;

import com.homefixer.notificacion.service.NotificacionService;
import com.homefixer.shared.dto.NotificacionEventDTO;
import com.homefixer.shared.messaging.MessagingConstants;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor asíncrono de eventos (solicitudes, pagos, valoraciones, chat) con ACK manual:
 * <ul>
 *   <li>Procesado OK  -> basicAck: RabbitMQ elimina el mensaje.</li>
 *   <li>Falla tras N intentos -> basicNack(requeue=false): RabbitMQ lo envía al DLX / DLQ.</li>
 * </ul>
 */
@Slf4j
@Component
public class NotificacionEventListener {

    /** Texto que permite forzar un fallo para demostrar el flujo hacia la DLQ. */
    static final String MARCA_FALLO_SIMULADO = "FORZAR_ERROR";

    private final int maxIntentos;
    private final NotificacionService notificacionService;

    public NotificacionEventListener(@Value("${homefixer.messaging.max-intentos:3}") int maxIntentos,
                                     NotificacionService notificacionService) {
        this.maxIntentos = maxIntentos;
        this.notificacionService = notificacionService;
    }

    @RabbitListener(queues = MessagingConstants.NOTIFICACION_QUEUE)
    public void procesarNotificacion(@Payload NotificacionEventDTO evento,
                                     @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                     Channel channel) throws IOException {
        log.info("Evento {} [{}] recibido para la solicitud ID: {}",
                evento.getEventoId(), routingKey, evento.getSolicitudId());

        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                enviarNotificacion(evento);
                channel.basicAck(deliveryTag, false);
                log.info("[ACK] Evento {} procesado y confirmado (intento {}/{})", evento.getEventoId(), intento, maxIntentos);
                return;
            } catch (Exception e) {
                log.warn("Intento {}/{} fallido para el evento {}: {}", intento, maxIntentos, evento.getEventoId(), e.getMessage());
                esperarAntesDeReintentar(intento);
            }
        }

        channel.basicNack(deliveryTag, false, false);
        log.error("[NACK] Evento {} rechazado tras {} intentos y enviado a la DLQ {}",
                evento.getEventoId(), maxIntentos, MessagingConstants.NOTIFICACION_DLQ);
    }

    private void enviarNotificacion(NotificacionEventDTO evento) {
        if (evento.getMensaje() != null && evento.getMensaje().contains(MARCA_FALLO_SIMULADO)) {
            throw new IllegalStateException("Fallo simulado del proveedor de notificaciones");
        }
        // Notificación in-app (campana del front); aquí también irían email / push
        int creadas = notificacionService.registrar(evento);
        log.info("Notificación [{}] guardada para {} destinatario(s): {}", evento.getTipoServicio(), creadas, evento.getMensaje());
    }

    private void esperarAntesDeReintentar(int intento) {
        if (intento >= maxIntentos) {
            return;
        }
        try {
            Thread.sleep(500L * intento); // backoff lineal
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
