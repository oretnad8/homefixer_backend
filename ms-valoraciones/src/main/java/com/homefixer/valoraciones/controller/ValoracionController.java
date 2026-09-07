package com.homefixer.valoraciones.controller;

import com.homefixer.valoraciones.entity.Valoracion;
import com.homefixer.valoraciones.service.ValoracionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/valoraciones")
@RequiredArgsConstructor
public class ValoracionController {

    private final ValoracionService valoracionService;

    @PostMapping
    public ResponseEntity<Valoracion> registrar(@RequestBody Valoracion valoracion) {
        return ResponseEntity.ok(valoracionService.registrarValoracion(valoracion));
    }

    @GetMapping("/tecnico/{tecnicoId}")
    public ResponseEntity<List<Valoracion>> getPorTecnico(@PathVariable Long tecnicoId) {
        return ResponseEntity.ok(valoracionService.getValoracionesByTecnico(tecnicoId));
    }
}
