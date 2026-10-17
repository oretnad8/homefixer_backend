package com.homefixer.solicitudes.controller;

import com.homefixer.solicitudes.entity.Solicitud;
import com.homefixer.shared.enums.EstadoSolicitud;
import com.homefixer.solicitudes.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;

    @PostMapping
    public ResponseEntity<Solicitud> createSolicitud(@RequestBody Solicitud solicitud) {
        return new ResponseEntity<>(solicitudService.createSolicitud(solicitud), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Solicitud> getSolicitud(@PathVariable("id") Long id) {
        return ResponseEntity.ok(solicitudService.getSolicitudById(id));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Solicitud>> getByCliente(@PathVariable("clienteId") Long clienteId) {
        return ResponseEntity.ok(solicitudService.getSolicitudesByCliente(clienteId));
    }

    @GetMapping("/tecnico/{tecnicoId}")
    public ResponseEntity<List<Solicitud>> getByTecnico(@PathVariable("tecnicoId") Long tecnicoId) {
        return ResponseEntity.ok(solicitudService.getSolicitudesByTecnico(tecnicoId));
    }

    /** Solicitudes nuevas, sin técnico, que el técnico no ha descartado. */
    @GetMapping("/disponibles")
    public ResponseEntity<List<Solicitud>> getDisponibles(@RequestParam("tecnicoId") Long tecnicoId) {
        return ResponseEntity.ok(solicitudService.getSolicitudesDisponibles(tecnicoId));
    }

    @GetMapping("/tecnico/{tecnicoId}/activas/count")
    public ResponseEntity<Long> countActivasByTecnico(@PathVariable("tecnicoId") Long tecnicoId) {
        return ResponseEntity.ok(solicitudService.countSolicitudesActivasByTecnico(tecnicoId));
    }

    @GetMapping("/tecnico/{tecnicoId}/estadisticas")
    public ResponseEntity<Map<String, Object>> getEstadisticas(@PathVariable("tecnicoId") Long tecnicoId) {
        return ResponseEntity.ok(solicitudService.estadisticasTecnico(tecnicoId));
    }

    /** El cliente elige técnico o el técnico acepta la solicitud. */
    @PatchMapping("/{id}/asignar-tecnico/{tecnicoId}")
    public ResponseEntity<Solicitud> assignTecnico(@PathVariable("id") Long id, @PathVariable("tecnicoId") Long tecnicoId) {
        return ResponseEntity.ok(solicitudService.assignTecnico(id, tecnicoId));
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<Void> rechazar(@PathVariable("id") Long id, @RequestParam("tecnicoId") Long tecnicoId) {
        solicitudService.rechazar(id, tecnicoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/iniciar")
    public ResponseEntity<Solicitud> iniciar(@PathVariable("id") Long id) {
        return ResponseEntity.ok(solicitudService.iniciar(id));
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<Solicitud> finalizar(@PathVariable("id") Long id,
                                               @RequestBody(required = false) Map<String, String> body) {
        return ResponseEntity.ok(solicitudService.finalizar(id, body != null ? body.get("notas") : null));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<Solicitud> cancelar(@PathVariable("id") Long id) {
        return ResponseEntity.ok(solicitudService.cancelar(id));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Solicitud> updateEstado(@PathVariable("id") Long id, @RequestParam("estado") EstadoSolicitud estado) {
        return ResponseEntity.ok(solicitudService.updateEstado(id, estado));
    }

    @GetMapping("/{id}/tecnicos-cercanos")
    public ResponseEntity<List<Long>> getNearbyTecnicos(@PathVariable("id") Long id) {
        return ResponseEntity.ok(solicitudService.findNearbyTecnicos(id));
    }
}
