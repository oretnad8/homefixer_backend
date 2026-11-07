package com.homefixer.solicitudes.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "ms-usuarios")
public interface UsuariosFeignClient {
    /** Técnico: nombre, telefono, especialidad, precioBase, calificacionPromedio. 404 si no es técnico. */
    @GetMapping("/api/tecnicos/{id}")
    Map<String, Object> obtenerTecnico(@PathVariable("id") Long id);

    /** Usuario dueño del token: { id, esCliente, esTecnico, ... }. 401/404 si el token no es válido. */
    @GetMapping("/api/usuarios/me")
    Map<String, Object> usuarioActual(@RequestHeader("Authorization") String authorization);
}
