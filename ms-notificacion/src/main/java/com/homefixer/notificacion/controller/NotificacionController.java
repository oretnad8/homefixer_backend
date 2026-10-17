package com.homefixer.notificacion.controller;

import com.homefixer.notificacion.entity.Notificacion;
import com.homefixer.notificacion.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    /** Últimas 50 notificaciones del usuario (generadas desde los eventos de RabbitMQ). */
    @GetMapping
    public ResponseEntity<List<Notificacion>> getNotificaciones(@RequestParam("usuarioId") Long usuarioId) {
        return ResponseEntity.ok(notificacionService.deUsuario(usuarioId));
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable("id") Long id) {
        notificacionService.marcarLeida(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/leidas")
    public ResponseEntity<Map<String, Integer>> marcarTodasLeidas(@RequestParam("usuarioId") Long usuarioId) {
        return ResponseEntity.ok(Map.of("actualizadas", notificacionService.marcarTodasLeidas(usuarioId)));
    }
}
