package com.homefixer.usuarios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-solicitudes")
public interface SolicitudesFeignClient {
    @GetMapping("/api/solicitudes/tecnico/{tecnicoId}/activas/count")
    long contarSolicitudesActivas(@PathVariable("tecnicoId") Long tecnicoId);
}
