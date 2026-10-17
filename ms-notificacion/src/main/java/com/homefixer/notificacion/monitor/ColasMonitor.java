package com.homefixer.notificacion.monitor;

import com.homefixer.shared.messaging.MessagingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Alertas de mensajería: revisa periódicamente las colas de eventos y sus DLQ y deja
 * una alerta en el log (nivel ERROR, prefijo [ALERTA]) cuando:
 * <ul>
 *   <li>una DLQ tiene mensajes (eventos que no se pudieron procesar),</li>
 *   <li>una cola principal acumula más mensajes que el umbral (consumidor lento),</li>
 *   <li>una cola principal no tiene consumidores conectados (servicio caído).</li>
 * </ul>
 */
@Slf4j
@Component
public class ColasMonitor {

    private static final List<String> COLAS_PRINCIPALES = List.of(
            MessagingConstants.NOTIFICACION_QUEUE, MessagingConstants.REPORTES_QUEUE,
            MessagingConstants.PAGOS_QUEUE, MessagingConstants.USUARIOS_VALORACIONES_QUEUE,
            MessagingConstants.SOLICITUDES_PAGOS_QUEUE);
    private static final List<String> DEAD_LETTER_QUEUES = List.of(
            MessagingConstants.NOTIFICACION_DLQ, MessagingConstants.REPORTES_DLQ,
            MessagingConstants.PAGOS_DLQ, MessagingConstants.USUARIOS_VALORACIONES_DLQ,
            MessagingConstants.SOLICITUDES_PAGOS_DLQ);

    private final AmqpAdmin amqpAdmin;
    private final int umbralDlq;
    private final int umbralColaPrincipal;

    public ColasMonitor(AmqpAdmin amqpAdmin,
                        @Value("${homefixer.messaging.alertas.umbral-dlq:1}") int umbralDlq,
                        @Value("${homefixer.messaging.alertas.umbral-cola-principal:1000}") int umbralColaPrincipal) {
        this.amqpAdmin = amqpAdmin;
        this.umbralDlq = umbralDlq;
        this.umbralColaPrincipal = umbralColaPrincipal;
    }

    @Scheduled(fixedDelayString = "${homefixer.messaging.alertas.intervalo-ms:60000}", initialDelay = 15000)
    public void revisarColas() {
        try {
            for (String cola : COLAS_PRINCIPALES) {
                QueueInformation info = amqpAdmin.getQueueInfo(cola);
                if (info == null) {
                    log.warn("[ALERTA] La cola {} no existe todavía (¿su microservicio consumidor no ha arrancado?)", cola);
                    continue;
                }
                if (info.getConsumerCount() == 0) {
                    log.error("[ALERTA] La cola {} no tiene consumidores conectados ({} mensajes pendientes)",
                            cola, info.getMessageCount());
                }
                if (info.getMessageCount() >= umbralColaPrincipal) {
                    log.error("[ALERTA] La cola {} acumula {} mensajes (umbral {})",
                            cola, info.getMessageCount(), umbralColaPrincipal);
                }
            }
            for (String dlq : DEAD_LETTER_QUEUES) {
                QueueInformation info = amqpAdmin.getQueueInfo(dlq);
                if (info != null && info.getMessageCount() >= umbralDlq) {
                    log.error("[ALERTA][DLQ] {} contiene {} mensajes fallidos. Revisar en http://localhost:15672/#/queues",
                            dlq, info.getMessageCount());
                }
            }
        } catch (Exception e) {
            log.error("[ALERTA] No se pudo consultar RabbitMQ: {}", e.getMessage());
        }
    }

    /** Estado actual de todas las colas monitoreadas (mensajes y consumidores). */
    public Map<String, Object> estadoColas() {
        Map<String, Object> estado = new LinkedHashMap<>();
        for (String cola : concatenar(COLAS_PRINCIPALES, DEAD_LETTER_QUEUES)) {
            QueueInformation info = amqpAdmin.getQueueInfo(cola);
            estado.put(cola, info == null
                    ? Map.of("existe", false)
                    : Map.of("existe", true,
                             "mensajes", info.getMessageCount(),
                             "consumidores", info.getConsumerCount()));
        }
        return estado;
    }

    private static List<String> concatenar(List<String> a, List<String> b) {
        return Stream.concat(a.stream(), b.stream()).toList();
    }
}
