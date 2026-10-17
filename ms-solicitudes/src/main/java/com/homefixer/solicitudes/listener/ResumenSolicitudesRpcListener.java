package com.homefixer.solicitudes.listener;

import com.homefixer.shared.dto.ResumenSolicitudesDTO;
import com.homefixer.shared.messaging.MessagingConstants;
import com.homefixer.solicitudes.repository.SolicitudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servidor RPC (mensajería síncrona request/reply). El valor de retorno se envía
 * automáticamente a la cola indicada en la cabecera reply_to del mensaje recibido.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumenSolicitudesRpcListener {

    private final SolicitudRepository solicitudRepository;

    @RabbitListener(queues = MessagingConstants.RPC_RESUMEN_SOLICITUDES_QUEUE)
    public ResumenSolicitudesDTO responderResumen(String solicitante) {
        log.info("[RPC] Petición de resumen de solicitudes recibida de {}", solicitante);

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (Object[] fila : solicitudRepository.contarPorEstado()) {
            porEstado.put(String.valueOf(fila[0]), (Long) fila[1]);
        }

        return ResumenSolicitudesDTO.builder()
                .totalSolicitudes(solicitudRepository.count())
                .solicitudesPorEstado(porEstado)
                .generadoEn(LocalDateTime.now())
                .build();
    }
}
