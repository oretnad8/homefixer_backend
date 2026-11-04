package com.homefixer.notificacion.service;

import com.homefixer.shared.kafka.SolicitudEventoKafka;
import com.homefixer.shared.kafka.TipoEventoKafka;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuditoriaEventosServiceTest {

    private final AuditoriaEventosService auditoria = new AuditoriaEventosService();

    private static SolicitudEventoKafka evento(String id, TipoEventoKafka tipo, String estado) {
        return new SolicitudEventoKafka(id, tipo, 1, Instant.now(), "test", 77L, 1L, 2L, "plomeria", null, estado, 1000.0, null);
    }

    @Test
    void reconstruyeHistorialEnOrdenSinAnomalias() {
        assertThat(auditoria.registrar(evento("e1", TipoEventoKafka.SOLICITUD_CREADA, "CREADA"))).isNull();
        assertThat(auditoria.registrar(evento("e2", TipoEventoKafka.TECNICO_ASIGNADO, "ACEPTADA"))).isNull();
        assertThat(auditoria.registrar(evento("e3", TipoEventoKafka.ESTADO_ACTUALIZADO, "COMPLETADA"))).isNull();
        assertThat(auditoria.registrar(evento("e4", TipoEventoKafka.PAGO_PROCESADO, "PAGADA"))).isNull();

        assertThat(auditoria.historial(77L)).extracting(AuditoriaEventosService.EntradaHistorial::estado)
                .containsExactly("CREADA", "ACEPTADA", "COMPLETADA", "PAGADA");
        assertThat(auditoria.resumen().eventosAuditados()).isEqualTo(4);
    }

    @Test
    void descartaDuplicados() {
        auditoria.registrar(evento("e1", TipoEventoKafka.SOLICITUD_CREADA, "CREADA"));
        auditoria.registrar(evento("e1", TipoEventoKafka.SOLICITUD_CREADA, "CREADA"));
        assertThat(auditoria.resumen().duplicadosDescartados()).isEqualTo(1);
        assertThat(auditoria.historial(77L)).hasSize(1);
    }

    @Test
    void detectaTransicionesInvalidas() {
        assertThat(AuditoriaEventosService.validarTransicion("COMPLETADA", "ACEPTADA")).contains("hacia atrás");
        assertThat(AuditoriaEventosService.validarTransicion("PAGADA", "EN_PROCESO")).contains("ya PAGADA");
        assertThat(AuditoriaEventosService.validarTransicion("EN_PROCESO", "CANCELADA")).isNull();
        assertThat(AuditoriaEventosService.validarTransicion("EN_PROCESO", "EN_PROCESO")).isNull();
        assertThat(AuditoriaEventosService.validarTransicion(null, "CREADA")).isNull();
    }
}
