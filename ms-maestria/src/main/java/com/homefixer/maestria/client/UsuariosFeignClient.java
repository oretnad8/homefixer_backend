package com.homefixer.maestria.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "ms-usuarios")
public interface UsuariosFeignClient {

    /** Usuario dueño del token: { id, nombre, esCliente, esTecnico, esAdmin }. */
    @GetMapping("/api/usuarios/me")
    Map<String, Object> usuarioActual(@RequestHeader("Authorization") String authorization);

    /** Perfil del técnico (nivelReputacion, especialidad, …). */
    @GetMapping("/api/tecnicos/{id}")
    Map<String, Object> obtenerTecnico(@PathVariable("id") Long id);
}
