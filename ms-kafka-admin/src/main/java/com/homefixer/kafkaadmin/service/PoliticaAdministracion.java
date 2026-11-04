package com.homefixer.kafkaadmin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reglas de gobernanza del administrador (sin dependencias de Kafka, testeables de forma unitaria):
 * qué configuraciones de tópico se pueden modificar, qué tópicos están protegidos y qué nombres son válidos.
 */
@Component
public class PoliticaAdministracion {

    /** Configuraciones de tópico modificables por la API (el resto queda fuera para evitar cambios peligrosos). */
    public static final Set<String> CONFIGS_PERMITIDAS = Set.of(
            "retention.ms", "retention.bytes", "cleanup.policy", "min.insync.replicas",
            "segment.ms", "segment.bytes", "max.message.bytes", "compression.type",
            "min.cleanable.dirty.ratio", "min.compaction.lag.ms", "delete.retention.ms",
            "unclean.leader.election.enable", "message.timestamp.type");

    private static final Set<String> CLEANUP_VALIDOS = Set.of("delete", "compact", "compact,delete", "delete,compact");

    private final Set<String> protegidos;

    public PoliticaAdministracion(@Value("${homefixer.kafka.admin.topicos-protegidos:}") String protegidos) {
        this.protegidos = Arrays.stream(protegidos.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    public void validarConfigs(Collection<String> claves) {
        Set<String> invalidas = claves.stream().filter(c -> !CONFIGS_PERMITIDAS.contains(c))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        if (!invalidas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Configuraciones no permitidas: " + invalidas + ". Permitidas: " + new java.util.TreeSet<>(CONFIGS_PERMITIDAS));
        }
    }

    public void validarValor(String clave, String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El valor de " + clave + " no puede estar vacío");
        }
        if ("cleanup.policy".equals(clave) && !CLEANUP_VALIDOS.contains(valor.replace(" ", ""))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cleanup.policy debe ser delete, compact o compact,delete");
        }
        if ("min.insync.replicas".equals(clave)) {
            int v = entero(clave, valor);
            if (v < 1 || v > 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "min.insync.replicas debe estar entre 1 y 3 (hay 3 brokers)");
            }
        }
        if (clave.endsWith(".ms") || clave.endsWith(".bytes")) {
            long v = largo(clave, valor);
            if (v < -1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, clave + " debe ser >= -1 (-1 = sin límite)");
            }
        }
    }

    public void validarEliminacion(String topico, boolean forzar) {
        if (esInterno(topico)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Los tópicos internos de Kafka no se pueden eliminar");
        }
        if (protegidos.contains(topico) && !forzar) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El tópico " + topico + " está protegido (lo usan los microservicios). Use ?forzar=true para eliminarlo.");
        }
    }

    public boolean esProtegido(String topico) {
        return protegidos.contains(topico);
    }

    public static boolean esInterno(String topico) {
        return topico.startsWith("__");
    }

    private static int entero(String clave, String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, clave + " debe ser numérico");
        }
    }

    private static long largo(String clave, String valor) {
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, clave + " debe ser numérico");
        }
    }
}
