package com.homefixer.notificacion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @PostMapping("/enviar")
    public ResponseEntity<String> enviarNotificacion(@RequestParam Long usuarioId, @RequestParam String mensaje) {
        // En una app real esto se integraría con Firebase, Email, SMS, etc.
        System.out.println("Notificación enviada a usuario " + usuarioId + ": " + mensaje);
        return ResponseEntity.ok("Notificación enviada exitosamente");
    }
}
