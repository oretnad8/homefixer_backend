package com.homefixer.ubicacion.service;

import com.homefixer.shared.kafka.UbicacionTecnicoEvento;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TrackingTecnicosServiceTest {

    private final TrackingTecnicosService tracking = new TrackingTecnicosService(120);

    private static UbicacionTecnicoEvento gps(double lat, double lng, Instant t) {
        return new UbicacionTecnicoEvento(java.util.UUID.randomUUID().toString(), 9001L, lat, lng, true, null, t, "test");
    }

    @Test
    void acumulaDistanciaYEstimaVelocidad() {
        Instant t0 = Instant.parse("2026-11-03T12:00:00Z");
        tracking.registrar(gps(-33.4500, -70.6600, t0));
        tracking.registrar(gps(-33.4590, -70.6600, t0.plusSeconds(60)));   // ~1 km en 1 min

        TrackingTecnicosService.EstadoTecnico e = tracking.estado(9001L);
        assertThat(e.reportes()).isEqualTo(2);
        assertThat(e.kmRecorridos()).isCloseTo(1.0, within(0.05));
        assertThat(e.velocidadKmH()).isCloseTo(60.0, within(3.0));
    }

    @Test
    void ignoraEventosFueraDeOrden() {
        Instant t0 = Instant.parse("2026-11-03T12:00:00Z");
        tracking.registrar(gps(-33.45, -70.66, t0.plusSeconds(60)));
        tracking.registrar(gps(-33.00, -70.00, t0));
        assertThat(tracking.estado(9001L).latitud()).isEqualTo(-33.45);
    }

    @Test
    void alertaInactividadUnaSolaVez() {
        Instant t0 = Instant.parse("2026-11-03T12:00:00Z");
        tracking.registrar(gps(-33.45, -70.66, t0));
        assertThat(tracking.detectarInactivos(t0.plusSeconds(60))).isEmpty();
        assertThat(tracking.detectarInactivos(t0.plusSeconds(200))).containsExactly(9001L);
        assertThat(tracking.detectarInactivos(t0.plusSeconds(300))).isEmpty();
    }
}
