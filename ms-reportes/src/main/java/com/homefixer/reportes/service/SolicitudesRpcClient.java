package com.homefixer.reportes.service;

import com.homefixer.shared.dto.ResumenSolicitudesDTO;
import com.homefixer.shared.messaging.MessagingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

/**
 * Cliente RPC (mensajería síncrona request/reply sobre RabbitMQ): envía la petición y
 * bloquea hasta recibir la respuesta en la cola de respuesta (direct reply-to) o hasta
 * que vence spring.rabbitmq.template.reply-timeout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudesRpcClient {

    private final RabbitTemplate rabbitTemplate;

    /** @return el resumen, o null si ms-solicitudes no respondió dentro del timeout. */
    public ResumenSolicitudesDTO obtenerResumen() {
        log.info("[RPC] Solicitando resumen a ms-solicitudes...");
        ResumenSolicitudesDTO resumen = rabbitTemplate.convertSendAndReceiveAsType(
                MessagingConstants.RPC_EXCHANGE,
                MessagingConstants.RK_RPC_RESUMEN_SOLICITUDES,
                "ms-reportes",
                new ParameterizedTypeReference<ResumenSolicitudesDTO>() {});
        if (resumen == null) {
            log.error("[ALERTA][RPC] ms-solicitudes no respondió a tiempo la petición de resumen");
        }
        return resumen;
    }
}
