package com.homefixer.notificacion.listener;

import com.homefixer.shared.dto.NotificacionEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificacionEventListener {

    public static final String NOTIFICACION_QUEUE = "notificacion.queue";

    @RabbitListener(queues = NOTIFICACION_QUEUE)
    public void procesarNotificacion(NotificacionEventDTO evento) {
        log.info("Mensaje recibido de RabbitMQ para la solicitud ID: {}", evento.getSolicitudId());
        
        try {
            // Aquí iría la lógica pesada (enviar emails, notificaciones push, websockets)
            log.info("Procesando evento de tipo [{}] - Mensaje: {}", evento.getTipoServicio(), evento.getMensaje());
            
            // Simulación de procesamiento
            Thread.sleep(1000);
            
            log.info("Notificación procesada exitosamente para Cliente ID: {}", evento.getClienteId());
        } catch (Exception e) {
            log.error("Error al procesar el mensaje de notificación. Evento: {}", evento, e);
            // Capturar la excepción evita que el mensaje vuelva a la cola infinitamente (poison message)
            // Alternativamente se podría lanzar AmqpRejectAndDontRequeueException
        }
    }
}
