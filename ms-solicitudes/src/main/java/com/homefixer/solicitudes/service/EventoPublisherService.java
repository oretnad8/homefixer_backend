package com.homefixer.solicitudes.service;

import com.homefixer.shared.dto.NotificacionEventDTO;
import com.homefixer.solicitudes.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventoPublisherService {

    private final RabbitTemplate rabbitTemplate;

    public void publicarNotificacion(NotificacionEventDTO evento) {
        log.info("Publicando evento de notificación para la solicitud ID: {}", evento.getSolicitudId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.NOTIFICACION_ROUTING_KEY,
                evento
        );
        log.info("Evento publicado con éxito.");
    }
}
