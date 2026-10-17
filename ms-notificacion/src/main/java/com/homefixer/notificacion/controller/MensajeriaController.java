package com.homefixer.notificacion.controller;

import com.homefixer.notificacion.monitor.ColasMonitor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones/mensajeria")
@RequiredArgsConstructor
public class MensajeriaController {

    private final ColasMonitor colasMonitor;

    /** Mensajes pendientes y consumidores de las colas de eventos y sus DLQ. */
    @GetMapping("/colas")
    public ResponseEntity<Map<String, Object>> estadoColas() {
        return ResponseEntity.ok(colasMonitor.estadoColas());
    }
}
