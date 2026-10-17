package com.homefixer.usuarios.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Los datos de prueba (mock_data.sql) insertan usuarios con id fijo, lo que deja la
 * secuencia de usuarios.id atrasada: el siguiente registro (p. ej. un login nuevo con
 * Google) choca con un id existente. Al arrancar se adelanta la secuencia al MAX(id).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecuenciaUsuariosMigracion implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Long siguiente = jdbcTemplate.queryForObject(
                    "SELECT setval(pg_get_serial_sequence('usuarios', 'id'), "
                            + "GREATEST((SELECT COALESCE(MAX(id), 0) FROM usuarios), 1))",
                    Long.class);
            log.info("Secuencia usuarios.id sincronizada (último id = {})", siguiente);
        } catch (Exception e) {
            log.warn("No se pudo sincronizar la secuencia de usuarios.id: {}", e.getMessage());
        }
    }
}
