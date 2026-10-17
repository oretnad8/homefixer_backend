package com.homefixer.usuarios.listener;

import com.homefixer.shared.dto.NotificacionEventDTO;
import com.homefixer.shared.messaging.MessagingConstants;
import com.homefixer.usuarios.service.UsuarioService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Suscriptor asíncrono de valoraciones: guarda el nuevo promedio del técnico. ACK/NACK manual. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ValoracionEventListener {

    private final UsuarioService usuarioService;

    @RabbitListener(queues = MessagingConstants.USUARIOS_VALORACIONES_QUEUE)
    public void onValoracion(@Payload NotificacionEventDTO evento,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                             Channel channel) throws IOException {
        try {
            // Solo las valoraciones de clientes a técnicos cambian la calificación del técnico
            if ("VALORACION_TECNICO".equals(evento.getTipoServicio()) && evento.getCalificacion() != null
                    && evento.getTecnicosIds() != null && !evento.getTecnicosIds().isEmpty()) {
                Long tecnicoId = evento.getTecnicosIds().get(0);
                usuarioService.actualizarCalificacion(tecnicoId, evento.getCalificacion(), evento.getTotalValoraciones());
                log.info("Calificación del técnico {} actualizada a {}", tecnicoId, evento.getCalificacion());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, false);
            log.error("[NACK] Evento {} enviado a la DLQ {}: {}", evento.getEventoId(),
                    MessagingConstants.USUARIOS_VALORACIONES_DLQ, e.getMessage());
        }
    }
}
