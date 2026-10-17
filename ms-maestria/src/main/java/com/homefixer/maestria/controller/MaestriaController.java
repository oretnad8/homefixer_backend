package com.homefixer.maestria.controller;

import com.homefixer.maestria.entity.Certificacion;
import com.homefixer.maestria.entity.InsigniaMaestria;
import com.homefixer.maestria.service.MaestriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Certificaciones e insignias de maestría.
 * <pre>
 *   POST /certificaciones                    (técnico)  sube una certificación -> PENDIENTE
 *   GET  /certificaciones/tecnico/{id}                  certificaciones del técnico con su estado
 *   GET  /certificaciones/pendientes         (admin)    por revisar
 *   PUT  /certificaciones/{id}/aprobar       (admin)    -> APROBADA + insignia
 *   PUT  /certificaciones/{id}/rechazar      (admin)    { motivo } -> RECHAZADA
 *   GET  /insignias/tecnico/{id}                        insignias del técnico
 *   GET  /insignias/lote?ids=1,2                        insignias de varios técnicos (ms-ubicacion)
 *   GET  /requisitos                                    nivel mínimo para postular
 * </pre>
 */
@RestController
@RequestMapping("/api/maestria")
@RequiredArgsConstructor
public class MaestriaController {

    private final MaestriaService maestriaService;

    public record CertificacionRequest(String titulo, String institucion, String categoria,
                                       LocalDate fechaEmision, String urlDocumento) {
    }

    @PostMapping("/certificaciones")
    public ResponseEntity<Certificacion> subirCertificacion(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CertificacionRequest body) {
        return ResponseEntity.ok(maestriaService.subirCertificacion(authorization, body.titulo(), body.institucion(),
                body.categoria(), body.fechaEmision(), body.urlDocumento()));
    }

    @GetMapping("/certificaciones/tecnico/{tecnicoId}")
    public ResponseEntity<List<Certificacion>> getCertificaciones(@PathVariable("tecnicoId") Long tecnicoId) {
        return ResponseEntity.ok(maestriaService.getCertificaciones(tecnicoId));
    }

    @GetMapping("/certificaciones/pendientes")
    public ResponseEntity<List<Certificacion>> getPendientes(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(maestriaService.getPendientes(authorization));
    }

    @PutMapping("/certificaciones/{id}/aprobar")
    public ResponseEntity<Certificacion> aprobar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(maestriaService.aprobar(authorization, id));
    }

    @PutMapping("/certificaciones/{id}/rechazar")
    public ResponseEntity<Certificacion> rechazar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(maestriaService.rechazar(authorization, id, body != null ? body.get("motivo") : null));
    }

    @GetMapping("/insignias/tecnico/{tecnicoId}")
    public ResponseEntity<List<InsigniaMaestria>> getInsignias(@PathVariable("tecnicoId") Long tecnicoId) {
        return ResponseEntity.ok(maestriaService.getInsignias(tecnicoId));
    }

    @GetMapping("/insignias/lote")
    public ResponseEntity<List<InsigniaMaestria>> getInsigniasLote(@RequestParam("ids") List<Long> ids) {
        return ResponseEntity.ok(maestriaService.getInsigniasDe(ids));
    }

    @GetMapping("/requisitos")
    public ResponseEntity<Map<String, String>> getRequisitos() {
        return ResponseEntity.ok(Map.of("nivelMinimo", maestriaService.getNivelMinimo()));
    }
}
