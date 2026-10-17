package com.homefixer.shared.messaging;

/**
 * Topología RabbitMQ compartida por todos los microservicios.
 *
 * <pre>
 *  Publish/Subscribe (asíncrono) — exchange topic durable [homefixer.events]
 *    Publicadores: ms-solicitudes (solicitud.*, chat.*), ms-pagos (pago.*), ms-valoraciones (valoracion.*),
 *                  ms-maestria (certificacion.*)
 *    Suscriptores (cola propia -> binding):
 *      ms-notificacion  notificacion.solicitudes.queue  <- solicitud.#, pago.#, valoracion.#, chat.#, certificacion.#
 *      ms-reportes      reportes.solicitudes.queue      <- solicitud.#
 *      ms-pagos         pagos.solicitudes.queue         <- solicitud.estado-actualizado
 *      ms-usuarios      usuarios.valoraciones.queue     <- valoracion.registrada
 *      ms-solicitudes   solicitudes.pagos.queue         <- pago.procesado (COMPLETADA -> PAGADA)
 *    Cada cola: ACK manual, durable, TTL + max-length, DLX [homefixer.dlx] -> su *.dlq
 *
 *  Request/Reply (síncrono) — exchange direct durable [homefixer.rpc]
 *    ms-reportes                -> solicitudes.rpc.resumen.queue -> ms-solicitudes
 *    ms-pagos / ms-valoraciones -> solicitudes.rpc.detalle.queue -> ms-solicitudes
 * </pre>
 */
public final class MessagingConstants {

    private MessagingConstants() {
    }

    // ---- Publish / Subscribe (asíncrono) ----
    public static final String EVENTS_EXCHANGE = "homefixer.events";

    public static final String RK_SOLICITUD_CREADA = "solicitud.creada";
    public static final String RK_SOLICITUD_TECNICO_ASIGNADO = "solicitud.tecnico-asignado";
    public static final String RK_SOLICITUD_ESTADO_ACTUALIZADO = "solicitud.estado-actualizado";
    public static final String RK_PAGO_PROCESADO = "pago.procesado";
    public static final String RK_VALORACION_REGISTRADA = "valoracion.registrada";
    public static final String RK_CHAT_MENSAJE = "chat.mensaje";
    public static final String RK_CERTIFICACION_SOLICITADA = "certificacion.solicitada";
    public static final String RK_CERTIFICACION_APROBADA = "certificacion.aprobada";
    public static final String RK_CERTIFICACION_RECHAZADA = "certificacion.rechazada";

    public static final String RK_SOLICITUD_ALL = "solicitud.#";
    public static final String RK_PAGO_ALL = "pago.#";
    public static final String RK_VALORACION_ALL = "valoracion.#";
    public static final String RK_CHAT_ALL = "chat.#";
    public static final String RK_CERTIFICACION_ALL = "certificacion.#";

    /** ms-notificacion: guarda una notificación por destinatario de cada evento. */
    public static final String NOTIFICACION_QUEUE = "notificacion.solicitudes.queue";
    /** ms-reportes: métricas de eventos de solicitudes. */
    public static final String REPORTES_QUEUE = "reportes.solicitudes.queue";
    /** ms-pagos: genera el cobro cuando una solicitud se completa. */
    public static final String PAGOS_QUEUE = "pagos.solicitudes.queue";
    /** ms-usuarios: actualiza la calificación promedio del técnico. */
    public static final String USUARIOS_VALORACIONES_QUEUE = "usuarios.valoraciones.queue";
    /** ms-solicitudes: marca la solicitud como PAGADA al procesarse su pago. */
    public static final String SOLICITUDES_PAGOS_QUEUE = "solicitudes.pagos.queue";

    // ---- Dead Letter Exchange / Queues ----
    public static final String DLX_EXCHANGE = "homefixer.dlx";
    public static final String NOTIFICACION_DLQ = "notificacion.solicitudes.dlq";
    public static final String REPORTES_DLQ = "reportes.solicitudes.dlq";
    public static final String PAGOS_DLQ = "pagos.solicitudes.dlq";
    public static final String USUARIOS_VALORACIONES_DLQ = "usuarios.valoraciones.dlq";
    public static final String SOLICITUDES_PAGOS_DLQ = "solicitudes.pagos.dlq";

    // ---- Request / Reply (síncrono sobre RabbitMQ) ----
    public static final String RPC_EXCHANGE = "homefixer.rpc";
    public static final String RPC_RESUMEN_SOLICITUDES_QUEUE = "solicitudes.rpc.resumen.queue";
    public static final String RK_RPC_RESUMEN_SOLICITUDES = "rpc.solicitudes.resumen";
    public static final String RPC_DETALLE_SOLICITUD_QUEUE = "solicitudes.rpc.detalle.queue";
    public static final String RK_RPC_DETALLE_SOLICITUD = "rpc.solicitudes.detalle";

    // ---- Políticas de retención ----
    /** Mensajes no consumidos en la cola principal expiran a las 24 h y pasan a la DLQ. */
    public static final int MAIN_QUEUE_TTL_MS = 24 * 60 * 60 * 1000;
    /** Máximo de mensajes en cola principal; el exceso se rechaza y se envía a la DLQ. */
    public static final int MAIN_QUEUE_MAX_LENGTH = 10_000;
    /** Los mensajes muertos se conservan 7 días para análisis / reprocesamiento. */
    public static final int DLQ_TTL_MS = 7 * 24 * 60 * 60 * 1000;
    public static final int DLQ_MAX_LENGTH = 50_000;
    /** Las peticiones RPC sin respuesta caducan rápido: nadie espera la respuesta. */
    public static final int RPC_QUEUE_TTL_MS = 10_000;
}
