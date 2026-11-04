package com.homefixer.notificacion.controller;

import com.homefixer.notificacion.service.AuditoriaEventosService;
import com.homefixer.notificacion.service.AuditoriaEventosService.EntradaHistorial;
import com.homefixer.notificacion.service.AuditoriaEventosService.ResumenAuditoria;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Auditoría construida desde el stream Kafka (consumer group notificacion-auditoria). */
@RestController
@RequestMapping("/api/notificaciones/kafka/auditoria")
@RequiredArgsConstructor
public class AuditoriaKafkaController {

    private final AuditoriaEventosService auditoria;

    @GetMapping
    public ResumenAuditoria resumen() {
        return auditoria.resumen();
    }

    /** Historial completo de una solicitud reconstruido desde los eventos, en orden. */
    @GetMapping("/solicitudes/{solicitudId}")
    public List<EntradaHistorial> historial(@PathVariable Long solicitudId) {
        List<EntradaHistorial> h = auditoria.historial(solicitudId);
        if (h.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay eventos auditados para la solicitud " + solicitudId);
        }
        return h;
    }
}
