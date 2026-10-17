package com.homefixer.pagos.service;

import com.homefixer.pagos.client.SolicitudesRpcClient;
import com.homefixer.pagos.entity.Pago;
import com.homefixer.pagos.repository.PagoRepository;
import com.homefixer.shared.dto.NotificacionEventDTO;
import com.homefixer.shared.dto.SolicitudDetalleDTO;
import com.homefixer.shared.enums.EstadoPago;
import com.homefixer.shared.messaging.EventPublisher;
import com.homefixer.shared.messaging.MessagingConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoService {

    private static final Set<String> METODOS = Set.of("tarjeta", "efectivo", "transferencia", "paypal");

    private final PagoRepository pagoRepository;
    private final SolicitudesRpcClient solicitudesRpcClient;
    private final EventPublisher eventPublisher;

    /** Endpoint original: registra un pago ya procesado. */
    public Pago procesarPago(Pago pago) {
        pago.setEstadoPago(EstadoPago.PROCESADO);
        pago.setFecha(LocalDateTime.now());
        pago.setComprobanteUrl("https://homefixer.com/receipts/" + UUID.randomUUID());
        return pagoRepository.save(pago);
    }

    /** Llamado por el consumidor del evento de solicitud COMPLETADA. Idempotente. */
    @Transactional
    public Pago generarCobro(Long solicitudId, Long clienteId, Long tecnicoId, Double monto, String categoria) {
        return pagoRepository.findFirstBySolicitudIdOrderByIdDesc(solicitudId).orElseGet(() -> {
            Pago pendiente = Pago.builder()
                    .solicitudId(solicitudId)
                    .clienteId(clienteId)
                    .tecnicoId(tecnicoId)
                    .descripcion("Servicio de " + (categoria != null ? categoria : "hogar") + " · solicitud #" + solicitudId)
                    .monto(monto != null ? monto : 0.0)
                    .estadoPago(EstadoPago.PENDIENTE)
                    .metodoPago("pendiente")
                    .fecha(LocalDateTime.now())
                    .build();
            log.info("Cobro pendiente generado para la solicitud {} por {}", solicitudId, pendiente.getMonto());
            return pagoRepository.save(pendiente);
        });
    }

    /**
     * Cobro de una solicitud. Normalmente ya lo generó el evento; si no (servicio completado
     * antes de existir este flujo, o el evento aún está en la cola) se consulta la solicitud por RPC.
     */
    @Transactional
    public Pago getOCrearPagoDeSolicitud(Long solicitudId) {
        return pagoRepository.findFirstBySolicitudIdOrderByIdDesc(solicitudId).orElseGet(() -> {
            SolicitudDetalleDTO s = solicitudesRpcClient.obtenerDetalle(solicitudId);
            if (!"COMPLETADA".equals(s.getEstado())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El servicio aún no está completado.");
            }
            return generarCobro(s.getId(), s.getClienteId(), s.getTecnicoId(), s.getPrecioEstimado(), s.getCategoria());
        });
    }

    @Transactional
    public Pago pagar(Long pagoId, String metodoPago) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado."));
        if (pago.getEstadoPago() == EstadoPago.PROCESADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este servicio ya fue pagado.");
        }
        if (metodoPago == null || !METODOS.contains(metodoPago)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Método de pago no válido.");
        }
        // Pasarela simulada: siempre aprueba
        pago.setMetodoPago(metodoPago);
        pago.setEstadoPago(EstadoPago.PROCESADO);
        pago.setFecha(LocalDateTime.now());
        pago.setComprobanteUrl("https://homefixer.com/receipts/" + UUID.randomUUID());
        Pago guardado = pagoRepository.save(pago);

        List<Long> destinatarios = new ArrayList<>();
        if (guardado.getTecnicoId() != null) destinatarios.add(guardado.getTecnicoId());
        if (guardado.getClienteId() != null) destinatarios.add(guardado.getClienteId());
        eventPublisher.publicar(MessagingConstants.RK_PAGO_PROCESADO, NotificacionEventDTO.builder()
                .solicitudId(guardado.getSolicitudId())
                .clienteId(guardado.getClienteId())
                .tecnicosIds(guardado.getTecnicoId() != null ? List.of(guardado.getTecnicoId()) : List.of())
                .destinatariosIds(destinatarios)
                .tipoServicio("PAGO_PROCESADO")
                .monto(guardado.getMonto())
                .mensaje("Pago de $" + Math.round(guardado.getMonto()) + " recibido por la solicitud #" + guardado.getSolicitudId() + ".")
                .build());
        return guardado;
    }

    public List<Pago> getPagosDeCliente(Long clienteId) {
        return pagoRepository.findByClienteIdOrderByFechaDesc(clienteId);
    }
}
