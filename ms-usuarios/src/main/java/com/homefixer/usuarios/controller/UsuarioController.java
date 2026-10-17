package com.homefixer.usuarios.controller;

import com.homefixer.usuarios.entity.Usuario;
import com.homefixer.usuarios.service.CambioRolService;
import com.homefixer.usuarios.service.MigracionCuentasService;
import com.homefixer.usuarios.service.SincronizacionService;
import com.homefixer.usuarios.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final CambioRolService cambioRolService;
    private final SincronizacionService sincronizacionService;
    private final MigracionCuentasService migracionCuentasService;

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> getUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.getUsuarioById(id));
    }

    @GetMapping
    public ResponseEntity<java.util.List<Usuario>> getAllUsuarios() {
        return ResponseEntity.ok(usuarioService.getAllUsuarios());
    }

    /**
     * Sincroniza el usuario autenticado en Supabase con la BD local (lo crea si no existe).
     * Respuesta: { id, nombre, email, telefono, esCliente, esTecnico, esAdmin }.
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> sincronizar(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(sincronizacionService.sincronizar(authorization));
    }

    /** Usuario del token y sus roles, sin crearlo (404 si nunca inició sesión). */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> yo(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(sincronizacionService.usuarioDelToken(authorization));
    }

    /** Crea en Supabase Auth las cuentas de los usuarios existentes (solo ADMIN vía gateway). */
    @PostMapping("/supabase/migrar-cuentas")
    public ResponseEntity<java.util.List<Map<String, Object>>> migrarCuentas() {
        return ResponseEntity.ok(migracionCuentasService.migrarCuentas());
    }

    /** Roles del usuario: { usuarioId, esCliente, esTecnico }. */
    @GetMapping("/{id}/roles")
    public ResponseEntity<Map<String, Object>> getRoles(@PathVariable("id") Long id) {
        return ResponseEntity.ok(cambioRolService.obtenerRoles(id));
    }
}
