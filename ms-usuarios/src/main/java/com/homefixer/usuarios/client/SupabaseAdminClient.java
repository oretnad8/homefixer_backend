package com.homefixer.usuarios.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Admin API de Supabase Auth (requiere la service_role key, nunca exponerla al front).
 * Crea cuentas ya confirmadas para que el usuario obtenga su token con email + contraseña.
 */
@Slf4j
@Component
public class SupabaseAdminClient {

    public enum Resultado { CREADO, YA_EXISTIA }

    /** Valor de usuarios.password cuando la contraseña la custodia Supabase (no se guarda en la BD local). */
    public static final String PASSWORD_GESTIONADA_POR_SUPABASE = "SUPABASE_AUTH";

    private final RestClient restClient;
    private final boolean configurado;

    public SupabaseAdminClient(@Value("${supabase.auth-url:}") String authUrl,
                               @Value("${supabase.service-role-key:}") String serviceRoleKey) {
        this.configurado = !authUrl.isBlank() && !serviceRoleKey.isBlank();
        this.restClient = RestClient.builder()
                .baseUrl(authUrl)
                .defaultHeader("apikey", serviceRoleKey)
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .build();
        if (!configurado) {
            log.warn("SUPABASE_SERVICE_ROLE_KEY no configurada: no se crearán cuentas en Supabase desde el backend");
        }
    }

    public boolean isConfigurado() {
        return configurado;
    }

    public Resultado crearCuenta(String email, String password, Map<String, Object> metadata) {
        if (!configurado) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta configurar SUPABASE_SERVICE_ROLE_KEY en ms-usuarios.");
        }
        try {
            restClient.post()
                    .uri("/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "email", email,
                            "password", password,
                            "email_confirm", true, // sin correo de confirmación
                            "user_metadata", metadata))
                    .retrieve()
                    .toBodilessEntity();
            return Resultado.CREADO;
        } catch (RestClientResponseException e) {
            String cuerpo = e.getResponseBodyAsString();
            if (e.getStatusCode().value() == 422 && cuerpo.contains("email_exists")) {
                return Resultado.YA_EXISTIA;
            }
            log.error("Supabase rechazó la cuenta {}: {} {}", email, e.getStatusCode(), cuerpo);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Supabase rechazó la cuenta " + email + ": " + cuerpo);
        }
    }
}
