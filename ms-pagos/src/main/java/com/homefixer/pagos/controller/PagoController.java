package com.homefixer.pagos.controller;

import com.homefixer.pagos.entity.Pago;
import com.homefixer.pagos.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/procesar")
    public ResponseEntity<Pago> procesarPago(@RequestBody Pago pago) {
        return ResponseEntity.ok(pagoService.procesarPago(pago));
    }

    /** Cobro de la solicitud (PENDIENTE hasta que el cliente paga). */
    @GetMapping("/solicitud/{solicitudId}")
    public ResponseEntity<Pago> getPagoPorSolicitud(@PathVariable("solicitudId") Long solicitudId) {
        return ResponseEntity.ok(pagoService.getOCrearPagoDeSolicitud(solicitudId));
    }

    /** Paga un cobro pendiente: { "metodoPago": "tarjeta|efectivo|transferencia|paypal" }. */
    @PostMapping("/{id}/procesar")
    public ResponseEntity<Pago> pagar(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(pagoService.pagar(id, body.get("metodoPago")));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Pago>> getPagosDeCliente(@PathVariable("clienteId") Long clienteId) {
        return ResponseEntity.ok(pagoService.getPagosDeCliente(clienteId));
    }
}
