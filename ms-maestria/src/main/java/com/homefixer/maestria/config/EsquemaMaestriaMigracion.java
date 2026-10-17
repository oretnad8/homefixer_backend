package com.homefixer.maestria.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Certificaciones creadas antes del flujo de revisión solo tienen "verificado":
 * se les asigna el estado equivalente (true -> APROBADA, resto -> PENDIENTE).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsquemaMaestriaMigracion implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int filas = jdbcTemplate.update("UPDATE certificaciones SET estado = CASE WHEN verificado THEN 'APROBADA' "
                    + "ELSE 'PENDIENTE' END, fecha_solicitud = COALESCE(fecha_solicitud, NOW()) WHERE estado IS NULL");
            if (filas > 0) log.info("{} certificación(es) antiguas migradas al flujo de revisión", filas);
        } catch (Exception e) {
            log.warn("No se pudo migrar certificaciones.estado: {}", e.getMessage());
        }
    }
}
