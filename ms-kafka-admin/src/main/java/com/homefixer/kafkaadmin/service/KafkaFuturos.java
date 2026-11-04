package com.homefixer.kafkaadmin.service;

import org.apache.kafka.common.KafkaFuture;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Espera el resultado de una operación del AdminClient. Las excepciones de Kafka (ApiException) se
 * relanzan tal cual para que {@code KafkaAdminExceptionHandler} las traduzca a un código HTTP.
 */
final class KafkaFuturos {

    private static final long TIMEOUT_S = 15;

    private KafkaFuturos() {
    }

    static <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(TIMEOUT_S, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(e.getCause());
        } catch (TimeoutException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Kafka no respondió en " + TIMEOUT_S + " s: revise que los brokers estén arriba");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Operación interrumpida");
        }
    }
}
