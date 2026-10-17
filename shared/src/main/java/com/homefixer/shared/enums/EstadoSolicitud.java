package com.homefixer.shared.enums;

/**
 * Ciclo de vida de una solicitud:
 * CREADA -> ACEPTADA (técnico asignado) -> EN_PROCESO -> COMPLETADA (pago solicitado) -> PAGADA
 *        \-> CANCELADA (por el cliente, antes de iniciar el trabajo)
 * PAGADA la fija ms-solicitudes al recibir el evento pago.procesado; recién ahí se puede valorar.
 */
public enum EstadoSolicitud {
    CREADA,
    BUSCANDO_TECNICO,
    ACEPTADA,
    EN_PROCESO,
    COMPLETADA,
    PAGADA,
    CANCELADA
}
