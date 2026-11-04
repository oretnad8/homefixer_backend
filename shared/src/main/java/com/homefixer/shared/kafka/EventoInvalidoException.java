package com.homefixer.shared.kafka;

/**
 * Payload que no cumple el contrato del evento. No es reintentable: el error handler lo envía
 * directo al Dead Letter Topic para no bloquear la partición.
 */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
