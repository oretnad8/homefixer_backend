package com.homefixer.reportes.controller;

import com.homefixer.reportes.service.MetricasEventosService;
import com.homefixer.reportes.service.SolicitudesRpcClient;
import com.homefixer.shared.dto.ResumenSolicitudesDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final SolicitudesRpcClient solicitudesRpcClient;
    private final MetricasEventosService metricasEventosService;

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

    /** Consulta síncrona (RPC sobre RabbitMQ) a ms-solicitudes. */
    @GetMapping("/solicitudes/resumen")
    public ResponseEntity<ResumenSolicitudesDTO> getResumenSolicitudes() {
        ResumenSolicitudesDTO resumen = solicitudesRpcClient.obtenerResumen();
        if (resumen == null) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
        }
        return ResponseEntity.ok(resumen);
    }

    /** Eventos de solicitudes recibidos de forma asíncrona (Publish/Subscribe). */
    @GetMapping("/solicitudes/eventos")
    public ResponseEntity<Map<String, Long>> getEventosSolicitudes() {
        return ResponseEntity.ok(metricasEventosService.eventosPorTipo());
    }
}
