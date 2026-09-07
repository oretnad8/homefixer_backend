package com.homefixer.ubicacion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/ubicacion")
public class UbicacionController {

    @GetMapping("/buscar")
    public ResponseEntity<List<Long>> buscarTecnicosCercanos(@RequestParam String coordenadas) {
        // Simulación: retornar lista de IDs de técnicos cercanos basados en coordenadas
        // En una implementación real, esto consultaría una DB espacial o Redis Geo
        List<Long> mockTecnicos = Arrays.asList(1L, 2L, 5L);
        return ResponseEntity.ok(mockTecnicos);
    }
}
