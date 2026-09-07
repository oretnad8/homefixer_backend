package com.homefixer.solicitudes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "ms-ubicacion")
public interface UbicacionFeignClient {
    @GetMapping("/api/ubicacion/buscar")
    List<Long> buscarTecnicosCercanos(@RequestParam("coordenadas") String coordenadas);
}
