package com.homefixer.reportes.service;

import com.homefixer.shared.dto.NotificacionEventDTO;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Contadores en memoria de los eventos de solicitudes recibidos por suscripción. */
@Service
public class MetricasEventosService {

    private final Map<String, AtomicLong> eventosPorTipo = new ConcurrentHashMap<>();

    public void registrar(String routingKey, NotificacionEventDTO evento) {
        eventosPorTipo.computeIfAbsent(routingKey, k -> new AtomicLong()).incrementAndGet();
    }

    public Map<String, Long> eventosPorTipo() {
        Map<String, Long> copia = new TreeMap<>();
        eventosPorTipo.forEach((tipo, contador) -> copia.put(tipo, contador.get()));
        return copia;
    }
}
