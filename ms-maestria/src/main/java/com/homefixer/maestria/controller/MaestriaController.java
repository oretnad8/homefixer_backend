package com.homefixer.maestria.controller;

import com.homefixer.maestria.entity.Certificacion;
import com.homefixer.maestria.entity.InsigniaMaestria;
import com.homefixer.maestria.service.MaestriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maestria")
@RequiredArgsConstructor
public class MaestriaController {

    private final MaestriaService maestriaService;

    @PostMapping("/certificaciones")
    public ResponseEntity<Certificacion> agregarCertificacion(@RequestBody Certificacion certificacion) {
        return ResponseEntity.ok(maestriaService.agregarCertificacion(certificacion));
    }

    @GetMapping("/certificaciones/tecnico/{tecnicoId}")
    public ResponseEntity<List<Certificacion>> getCertificaciones(@PathVariable Long tecnicoId) {
        return ResponseEntity.ok(maestriaService.getCertificaciones(tecnicoId));
    }

    @PostMapping("/insignias")
    public ResponseEntity<InsigniaMaestria> otorgarInsignia(@RequestBody InsigniaMaestria insignia) {
        return ResponseEntity.ok(maestriaService.otorgarInsignia(insignia));
    }

    @GetMapping("/insignias/tecnico/{tecnicoId}")
    public ResponseEntity<List<InsigniaMaestria>> getInsignias(@PathVariable Long tecnicoId) {
        return ResponseEntity.ok(maestriaService.getInsignias(tecnicoId));
    }
}
