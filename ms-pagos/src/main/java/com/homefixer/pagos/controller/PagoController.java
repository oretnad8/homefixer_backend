package com.homefixer.pagos.controller;

import com.homefixer.pagos.entity.Pago;
import com.homefixer.pagos.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/procesar")
    public ResponseEntity<Pago> procesarPago(@RequestBody Pago pago) {
        return ResponseEntity.ok(pagoService.procesarPago(pago));
    }

    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<Pago> getPagoPorSolicitud(@PathVariable Long solicitudId) {
        return ResponseEntity.ok(pagoService.getPagoBySolicitud(solicitudId));
    }
}
