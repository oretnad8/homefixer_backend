package com.homefixer.reportes.controller;

import com.homefixer.reportes.dto.MetricasTiempoRealDTO;
import com.homefixer.reportes.service.MetricasStreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Métricas en tiempo real calculadas desde los tópicos Kafka.
 * <ul>
 *   <li>GET /api/reportes/metricas/tiempo-real: snapshot JSON.</li>
 *   <li>GET /api/reportes/stream: Server-Sent Events; empuja el snapshot cada vez que llegan eventos
 *       nuevos (como máximo 1 vez por segundo) y un heartbeat cada 15 s.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class StreamingController {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final MetricasStreamingService metricas;
    private final List<SseEmitter> clientes = new CopyOnWriteArrayList<>();
    private final AtomicLong ultimaVersionEnviada = new AtomicLong(-1);
    private final AtomicLong ticks = new AtomicLong();

    @GetMapping("/metricas/tiempo-real")
    public ResponseEntity<MetricasTiempoRealDTO> tiempoReal() {
        return ResponseEntity.ok(metricas.snapshot());
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onCompletion(() -> clientes.remove(emitter));
        emitter.onTimeout(() -> clientes.remove(emitter));
        emitter.onError(e -> clientes.remove(emitter));
        clientes.add(emitter);
        enviar(emitter, metricas.snapshot());   // estado inicial inmediato
        log.info("[SSE] Cliente conectado al stream de métricas ({} activos)", clientes.size());
        return emitter;
    }

    @Scheduled(fixedRate = 1000)
    public void empujar() {
        if (clientes.isEmpty()) {
            return;
        }
        long version = metricas.version();
        boolean heartbeat = ticks.incrementAndGet() % 15 == 0;
        if (version == ultimaVersionEnviada.get() && !heartbeat) {
            return;
        }
        ultimaVersionEnviada.set(version);
        MetricasTiempoRealDTO snapshot = metricas.snapshot();
        clientes.forEach(c -> enviar(c, snapshot));
    }

    private void enviar(SseEmitter emitter, MetricasTiempoRealDTO snapshot) {
        try {
            emitter.send(SseEmitter.event().name("metricas").data(snapshot, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            clientes.remove(emitter);
            emitter.completeWithError(e);
        }
    }
}
