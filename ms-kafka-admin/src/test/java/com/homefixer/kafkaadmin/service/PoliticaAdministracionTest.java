package com.homefixer.kafkaadmin.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaAdministracionTest {

    private final PoliticaAdministracion politica =
            new PoliticaAdministracion("homefixer.solicitudes.eventos, homefixer.tecnicos.ubicacion");

    @Test
    void aceptaConfiguracionesPermitidas() {
        assertThatCode(() -> politica.validarConfigs(List.of("retention.ms", "cleanup.policy", "min.insync.replicas")))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaConfiguracionesFueraDeLaListaBlanca() {
        assertThatThrownBy(() -> politica.validarConfigs(List.of("retention.ms", "leader.replication.throttled.replicas")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("leader.replication.throttled.replicas");
    }

    @Test
    void validaValores() {
        assertThatCode(() -> politica.validarValor("cleanup.policy", "compact,delete")).doesNotThrowAnyException();
        assertThatCode(() -> politica.validarValor("retention.ms", "-1")).doesNotThrowAnyException();
        assertThatThrownBy(() -> politica.validarValor("cleanup.policy", "borrar")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> politica.validarValor("min.insync.replicas", "4")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> politica.validarValor("retention.ms", "siete-dias")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> politica.validarValor("retention.ms", " ")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void protegeTopicosDelCasoSalvoForzar() {
        assertThat(politica.esProtegido("homefixer.tecnicos.ubicacion")).isTrue();
        assertThatThrownBy(() -> politica.validarEliminacion("homefixer.solicitudes.eventos", false))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThatCode(() -> politica.validarEliminacion("homefixer.solicitudes.eventos", true)).doesNotThrowAnyException();
        assertThatCode(() -> politica.validarEliminacion("demo.topico", false)).doesNotThrowAnyException();
    }

    @Test
    void nuncaEliminaTopicosInternos() {
        assertThatThrownBy(() -> politica.validarEliminacion("__consumer_offsets", true))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
