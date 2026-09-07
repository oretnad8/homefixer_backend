package com.homefixer.solicitudes.controller;

import com.homefixer.solicitudes.entity.Solicitud;
import com.homefixer.shared.enums.EstadoSolicitud;
import com.homefixer.solicitudes.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<Solicitud> getSolicitud(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.getSolicitudById(id));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Solicitud>> getByCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(solicitudService.getSolicitudesByCliente(clienteId));
    }

    @GetMapping("/tecnico/{tecnicoId}")
    public ResponseEntity<List<Solicitud>> getByTecnico(@PathVariable Long tecnicoId) {
        return ResponseEntity.ok(solicitudService.getSolicitudesByTecnico(tecnicoId));
    }

    @PatchMapping("/{id}/asignar-tecnico/{tecnicoId}")
    public ResponseEntity<Solicitud> assignTecnico(@PathVariable Long id, @PathVariable Long tecnicoId) {
        return ResponseEntity.ok(solicitudService.assignTecnico(id, tecnicoId));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Solicitud> updateEstado(@PathVariable Long id, @RequestParam EstadoSolicitud estado) {
        return ResponseEntity.ok(solicitudService.updateEstado(id, estado));
    }

    @GetMapping("/{id}/tecnicos-cercanos")
    public ResponseEntity<List<Long>> getNearbyTecnicos(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.findNearbyTecnicos(id));
    }
}
