package com.homefixer.ubicacion.controller;

import com.homefixer.ubicacion.dto.ReporteUbicacion;
import com.homefixer.ubicacion.dto.TecnicoCercanoDTO;
import com.homefixer.ubicacion.dto.UbicacionDTO;
import com.homefixer.ubicacion.service.UbicacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ubicacion")
@RequiredArgsConstructor
public class UbicacionController {

    private final UbicacionService ubicacionService;

    /** El técnico reporta su posición GPS (cada pocos segundos mientras está disponible). */
    @PutMapping("/tecnicos/{tecnicoId}")
    public ResponseEntity<Void> reportar(@PathVariable("tecnicoId") Long tecnicoId, @Valid @RequestBody ReporteUbicacion reporte) {
        ubicacionService.reportar(tecnicoId, reporte);
        return ResponseEntity.noContent().build();
    }

    /** El técnico deja de compartir su ubicación (se desconecta). */
    @DeleteMapping("/tecnicos/{tecnicoId}")
    public ResponseEntity<Void> desconectar(@PathVariable("tecnicoId") Long tecnicoId) {
        ubicacionService.desconectar(tecnicoId);
        return ResponseEntity.noContent().build();
    }

    /** Posición actual de un técnico (seguimiento en vivo del servicio). */
    @GetMapping("/tecnicos/{tecnicoId}")
    public ResponseEntity<UbicacionDTO> obtener(@PathVariable("tecnicoId") Long tecnicoId) {
        return ResponseEntity.ok(ubicacionService.obtener(tecnicoId));
    }

    /** Técnicos en línea de la categoría pedida, ordenados por distancia. */
    @GetMapping("/tecnicos-cercanos")
    public ResponseEntity<List<TecnicoCercanoDTO>> tecnicosCercanos(
            @RequestParam("lat") double lat,
            @RequestParam("lng") double lng,
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "radioKm", required = false) Double radioKm,
            @RequestParam(value = "excluirId", required = false) Long excluirId) {
        return ResponseEntity.ok(ubicacionService.buscarCercanos(lat, lng, categoria, radioKm, excluirId));
    }

    /** Ids de técnicos cercanos (uso interno de ms-solicitudes para notificar). coordenadas = "lat,lng". */
    @GetMapping("/buscar")
    public ResponseEntity<List<Long>> buscarTecnicosCercanos(
            @RequestParam("coordenadas") String coordenadas,
            @RequestParam(value = "categoria", required = false) String categoria) {
        return ResponseEntity.ok(ubicacionService.buscarIdsCercanos(coordenadas, categoria));
    }
}
