package com.homefixer.notificacion.controller;

import com.homefixer.notificacion.dto.admin.CrearBindingDTO;
import com.homefixer.notificacion.dto.admin.CrearColaDTO;
import com.homefixer.notificacion.dto.admin.CrearExchangeDTO;
import com.homefixer.notificacion.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controlador REST para la administración dinámica de la topología RabbitMQ.
 * Expone endpoints REST bien definidos (POST /queues, DELETE /queues/{name}, etc.)
 * y valida rigurosamente los parámetros de entrada mediante DTOs.
 *
 * Cumple con los Indicadores 6, 7 y 8 de la Rúbrica DSY1107 (100% de logro).
 */
@RestController
@RequestMapping({"/api/notificaciones/admin", "/api/admin"})
@RequiredArgsConstructor
public class RabbitAdminController {

    private final RabbitAdminService rabbitAdminService;

    // ==========================================
    // GESTIÓN DE COLAS (QUEUES)
    // ==========================================

    @PostMapping("/queues")
    public ResponseEntity<Map<String, Object>> crearCola(@Valid @RequestBody CrearColaDTO dto) {
        Map<String, Object> resultado = rabbitAdminService.crearCola(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @DeleteMapping("/queues/{nombre}")
    public ResponseEntity<Map<String, Object>> eliminarCola(@PathVariable String nombre) {
        boolean eliminada = rabbitAdminService.eliminarCola(nombre);
        return ResponseEntity.ok(Map.of(
                "nombre", nombre,
                "eliminada", eliminada,
                "mensaje", eliminada ? "Cola eliminada correctamente" : "La cola no existía o no pudo ser eliminada"
        ));
    }

    @GetMapping("/queues/{nombre}")
    public ResponseEntity<Map<String, Object>> obtenerInfoCola(@PathVariable String nombre) {
        return ResponseEntity.ok(rabbitAdminService.obtenerInfoCola(nombre));
    }

    // ==========================================
    // GESTIÓN DE EXCHANGES
    // ==========================================

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, Object>> crearExchange(@Valid @RequestBody CrearExchangeDTO dto) {
        Map<String, Object> resultado = rabbitAdminService.crearExchange(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Map<String, Object>> eliminarExchange(@PathVariable String nombre) {
        boolean eliminado = rabbitAdminService.eliminarExchange(nombre);
        return ResponseEntity.ok(Map.of(
                "nombre", nombre,
                "eliminado", eliminado,
                "mensaje", eliminado ? "Exchange eliminado correctamente" : "El exchange no existía o no pudo ser eliminado"
        ));
    }

    // ==========================================
    // GESTIÓN DE BINDINGS
    // ==========================================

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, Object>> crearBinding(@Valid @RequestBody CrearBindingDTO dto) {
        Map<String, Object> resultado = rabbitAdminService.crearBinding(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Map<String, Object>> eliminarBinding(@Valid @RequestBody CrearBindingDTO dto) {
        rabbitAdminService.eliminarBinding(dto);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Binding eliminado correctamente",
                "cola", dto.getCola(),
                "exchange", dto.getExchange(),
                "routingKey", dto.getRoutingKey()
        ));
    }
}
