package com.homefixer.reportes.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    @GetMapping("/plataforma/resumen")
    public ResponseEntity<Map<String, Object>> getResumenPlataforma() {
        // Este microservicio idealmente se comunicaría con Pagos, Usuarios, Solicitudes 
        // para consolidar los datos, o consumiría eventos asíncronos.
        Map<String, Object> resumen = Map.of(
                "totalUsuariosActivos", 1500,
                "serviciosCompletados", 320,
                "ingresosTotalesGenerados", 45000.00
        );
        return ResponseEntity.ok(resumen);
    }
}
