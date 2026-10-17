package com.homefixer.usuarios.service;

import com.homefixer.usuarios.client.SupabaseAdminClient;
import com.homefixer.usuarios.entity.Usuario;
import com.homefixer.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Crea en Supabase Auth las cuentas de los usuarios que existían solo en la BD local,
 * con su email y la contraseña guardada en usuarios.password. Es idempotente: las
 * cuentas que ya existen en Supabase se informan como YA_EXISTIA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigracionCuentasService {

    /** Supabase exige contraseñas de al menos 6 caracteres. */
    private static final int LARGO_MINIMO_PASSWORD = 6;

    private final UsuarioRepository usuarioRepository;
    private final SupabaseAdminClient supabaseAdminClient;

    public List<Map<String, Object>> migrarCuentas() {
        if (!supabaseAdminClient.isConfigurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta configurar SUPABASE_SERVICE_ROLE_KEY en ms-usuarios.");
        }
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Usuario usuario : usuarioRepository.findAll()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("id", usuario.getId());
            fila.put("email", usuario.getEmail());
            fila.put("estado", migrar(usuario));
            resultado.add(fila);
        }
        return resultado;
    }

    private String migrar(Usuario usuario) {
        String password = usuario.getPassword();
        if (SupabaseAdminClient.PASSWORD_GESTIONADA_POR_SUPABASE.equals(password)) {
            return "YA_EN_SUPABASE";
        }
        if (password == null || password.length() < LARGO_MINIMO_PASSWORD) {
            return "ERROR: contraseña de menos de " + LARGO_MINIMO_PASSWORD + " caracteres";
        }
        try {
            return supabaseAdminClient.crearCuenta(usuario.getEmail().trim().toLowerCase(), password,
                    Map.of("nombre", usuario.getNombre(), "telefono", usuario.getTelefono())).name();
        } catch (Exception e) {
            log.error("No se pudo migrar {}: {}", usuario.getEmail(), e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
