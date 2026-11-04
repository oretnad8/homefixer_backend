package com.homefixer.ubicacion.controller;

import com.homefixer.ubicacion.service.TrackingTecnicosService;
import com.homefixer.ubicacion.service.TrackingTecnicosService.EstadoTecnico;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Seguimiento de técnicos construido desde el stream Kafka (consumer group ubicacion-tracking). */
@RestController
@RequestMapping("/api/ubicacion/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingTecnicosService tracking;

    @GetMapping("/tecnicos")
    public List<EstadoTecnico> tecnicos() {
        return tracking.estados();
    }

    @GetMapping("/tecnicos/{tecnicoId}")
    public EstadoTecnico tecnico(@PathVariable Long tecnicoId) {
        EstadoTecnico e = tracking.estado(tecnicoId);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sin datos de tracking para el técnico " + tecnicoId);
        }
        return e;
    }
}
