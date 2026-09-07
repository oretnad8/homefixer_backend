package com.homefixer.promociones.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promociones")
public class PromocionController {

    @GetMapping("/activas")
    public ResponseEntity<List<String>> getPromocionesActivas() {
        return ResponseEntity.ok(List.of(
                "Descuento 20% en primer servicio",
                "Plan Premium Anual con 3 meses gratis"
        ));
    }
    
    @PostMapping("/aplicar")
    public ResponseEntity<String> aplicarCodigo(@RequestParam String codigo) {
        if ("BIENVENIDA20".equalsIgnoreCase(codigo)) {
            return ResponseEntity.ok("Código aplicado exitosamente");
        }
        return ResponseEntity.badRequest().body("Código inválido o expirado");
    }
}
