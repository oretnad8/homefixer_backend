package com.homefixer.valoraciones.controller;

import com.homefixer.valoraciones.entity.Valoracion;
import com.homefixer.valoraciones.service.ValoracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/valoraciones")
@RequiredArgsConstructor
public class ValoracionController {

    private final ValoracionService valoracionService;

    public record ValoracionRequest(Long solicitudId, Integer calificacion, String comentario, String autorRol) {
    }

    /** { solicitudId, calificacion (1-5), comentario, autorRol: "cliente" | "tecnico" } */
    @PostMapping
    public ResponseEntity<Valoracion> registrar(@RequestBody ValoracionRequest req) {
        return new ResponseEntity<>(valoracionService.registrar(req.solicitudId(), req.calificacion(),
                req.comentario(), req.autorRol()), HttpStatus.CREATED);
    }

    @GetMapping("/tecnico/{tecnicoId}")
    public ResponseEntity<List<Valoracion>> getPorTecnico(@PathVariable("tecnicoId") Long tecnicoId) {
        return ResponseEntity.ok(valoracionService.getValoracionesByTecnico(tecnicoId));
    }

    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<List<Valoracion>> getPorSolicitud(@PathVariable("solicitudId") Long solicitudId) {
        return ResponseEntity.ok(valoracionService.getValoracionesBySolicitud(solicitudId));
    }
}
