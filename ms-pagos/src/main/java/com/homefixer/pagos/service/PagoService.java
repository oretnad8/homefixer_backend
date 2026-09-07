package com.homefixer.pagos.service;

import com.homefixer.pagos.entity.Pago;
import com.homefixer.shared.enums.EstadoPago;
import com.homefixer.pagos.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagoService {
    private final PagoRepository pagoRepository;

    public Pago procesarPago(Pago pago) {
        // Simular procesamiento de pasarela
        pago.setEstadoPago(EstadoPago.PROCESADO);
        pago.setComprobanteUrl("https://homefixer.com/receipts/" + UUID.randomUUID().toString());
        return pagoRepository.save(pago);
    }

    public Pago getPagoBySolicitud(Long solicitudId) {
        return pagoRepository.findBySolicitudId(solicitudId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado para solicitud"));
    }
}
