package com.homefixer.valoraciones.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-usuarios")
public interface UsuarioFeignClient {
    // Definimos un endpoint virtual en usuarios o asumimos que se agregará luego
    @PutMapping("/api/tecnicos/{id}/calificacion")
    void actualizarCalificacion(@PathVariable("id") Long id, @RequestBody Double nuevaCalificacion);
}
