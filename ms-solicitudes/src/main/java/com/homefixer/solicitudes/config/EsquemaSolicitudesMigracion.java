package com.homefixer.solicitudes.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate creó un CHECK con los valores originales del enum EstadoSolicitud y
 * ddl-auto=update no lo actualiza. Se elimina para admitir ACEPTADA y CANCELADA
 * (el enum de Java sigue validando los valores).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsquemaSolicitudesMigracion implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute("ALTER TABLE solicitudes DROP CONSTRAINT IF EXISTS solicitudes_estado_solicitud_check");
        } catch (Exception e) {
            log.warn("No se pudo actualizar el CHECK de solicitudes.estado_solicitud: {}", e.getMessage());
        }
    }
}
