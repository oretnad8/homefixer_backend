package com.homefixer.shared.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homefixer.shared.dto.NotificacionEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Publica eventos de dominio en homefixer.events (Publish/Subscribe, asíncrono):
 * mensajes persistentes, publisher confirms (ACK del broker) y returns de mensajes no enrutables.
 * Requiere spring.rabbitmq.publisher-confirm-type=correlated y publisher-returns=true.
 */
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** RabbitTemplate JSON con callbacks de confirmación (ACK/NACK del broker) y de mensajes devueltos. */
    public static RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        template.setConfirmCallback((correlationData, ack, cause) -> {
            String id = correlationData != null ? correlationData.getId() : "N/A";
            if (ack) {
                log.info("[CONFIRM] Broker confirmó (ACK) el evento {}", id);
            } else {
                log.error("[ALERTA][CONFIRM] Broker rechazó (NACK) el evento {}: {}", id, cause);
            }
        });
        template.setMandatory(true);
        template.setReturnsCallback(returned -> log.error(
                "[ALERTA][RETURN] Mensaje no enrutable. exchange={}, routingKey={}, replyCode={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText()));
        return template;
    }

    public void publicar(String routingKey, NotificacionEventDTO evento) {
        if (evento.getEventoId() == null) {
            evento.setEventoId(UUID.randomUUID().toString());
        }
        if (evento.getFechaEvento() == null) {
            evento.setFechaEvento(LocalDateTime.now());
        }
        log.info("Publicando evento {} [{}] solicitud={}", evento.getEventoId(), routingKey, evento.getSolicitudId());
        try {
            rabbitTemplate.convertAndSend(MessagingConstants.EVENTS_EXCHANGE, routingKey, evento,
                    message -> {
                        // Durabilidad: el broker escribe el mensaje a disco (sobrevive a reinicios)
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        message.getMessageProperties().setMessageId(evento.getEventoId());
                        message.getMessageProperties().setType(routingKey);
                        return message;
                    },
                    new CorrelationData(evento.getEventoId()));
        } catch (AmqpException e) {
            // Broker caído: la operación de negocio ya se guardó, no se revierte por no poder notificar
            log.error("[ALERTA] No se pudo publicar el evento {} en RabbitMQ: {}", evento.getEventoId(), e.getMessage());
        }
    }
}
