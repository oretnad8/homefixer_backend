package com.homefixer.shared.kafka;

/**
 * Tópicos Kafka de HomeFixer (creados por kafka/create-topics.sh, 3 particiones y 3 réplicas).
 *
 * <pre>
 *  homefixer.solicitudes.eventos   key=solicitudId  delete, 7 días       productores: solicitudes, pagos, valoraciones
 *  homefixer.tecnicos.ubicacion    key=tecnicoId    compact,delete, 24 h productor:  ubicacion (+ simulador GPS)
 *  *.DLT                           mensajes que fallaron tras los reintentos (14 días)
 * </pre>
 *
 * RabbitMQ sigue a cargo del workflow transaccional (cobro, pago, notificaciones, RPC);
 * Kafka transporta el stream de eventos para analítica en tiempo real, auditoría y replay.
 */
public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String SOLICITUDES_EVENTOS = "homefixer.solicitudes.eventos";
    public static final String TECNICOS_UBICACION = "homefixer.tecnicos.ubicacion";

    public static final String DLT_SUFFIX = ".DLT";
    public static final String SOLICITUDES_EVENTOS_DLT = SOLICITUDES_EVENTOS + DLT_SUFFIX;
    public static final String TECNICOS_UBICACION_DLT = TECNICOS_UBICACION + DLT_SUFFIX;

    // ---- Consumer groups ----
    /** ms-reportes: métricas en tiempo real (ambos tópicos). Varias instancias se reparten las particiones. */
    public static final String GROUP_REPORTES_METRICAS = "reportes-metricas";
    /** ms-notificacion: auditoría de eventos de negocio (lee el mismo tópico con offsets propios). */
    public static final String GROUP_NOTIFICACION_AUDITORIA = "notificacion-auditoria";
    /** ms-ubicacion: tracking de la última posición de cada técnico. */
    public static final String GROUP_UBICACION_TRACKING = "ubicacion-tracking";
    /** ms-kafka-admin: lectura de los Dead Letter Topics. */
    public static final String GROUP_DLT_AUDITORIA = "dlt-auditoria";

    /** Header con el id del evento (trazabilidad e idempotencia sin deserializar el payload). */
    public static final String HEADER_EVENTO_ID = "eventoId";

    /**
     * Valor para spring.json.type.mapping (productor y consumidor): el header __TypeId__ viaja con un token
     * corto en vez del nombre de la clase, así productor y consumidor no dependen del paquete Java.
     */
    public static final String JSON_TYPE_MAPPING =
            "solicitudEvento:com.homefixer.shared.kafka.SolicitudEventoKafka,"
                    + "ubicacionEvento:com.homefixer.shared.kafka.UbicacionTecnicoEvento";
}
