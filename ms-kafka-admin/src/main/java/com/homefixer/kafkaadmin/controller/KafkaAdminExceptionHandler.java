package com.homefixer.kafkaadmin.controller;

import com.homefixer.shared.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/** Traduce los errores del AdminClient de Kafka y de validación a respuestas JSON con el código HTTP correcto. */
@Slf4j
@RestControllerAdvice
public class KafkaAdminExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> kafka(ApiException e) {
        HttpStatus status;
        if (e instanceof TopicExistsException || e instanceof GroupNotEmptyException) {
            status = HttpStatus.CONFLICT;
        } else if (e instanceof UnknownTopicOrPartitionException || e instanceof GroupIdNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (e instanceof InvalidPartitionsException || e instanceof InvalidReplicationFactorException
                || e instanceof InvalidConfigurationException || e instanceof InvalidTopicException
                || e instanceof InvalidRequestException || e instanceof PolicyViolationException) {
            status = HttpStatus.BAD_REQUEST;
        } else if (e instanceof TimeoutException) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else {
            status = HttpStatus.BAD_GATEWAY;
        }
        log.warn("[KAFKA-ADMIN] {} -> {}: {}", e.getClass().getSimpleName(), status.value(), e.getMessage());
        return respuesta(status, e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> status(ResponseStatusException e) {
        return respuesta(e.getStatusCode(), e.getReason());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {
        String errores = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respuesta(HttpStatus.BAD_REQUEST, "Error de validación: " + errores);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> peticionInvalida(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private static ResponseEntity<ErrorResponse> respuesta(HttpStatusCode status, String mensaje) {
        return ResponseEntity.status(status).body(ErrorResponse.builder()
                .status(status.value())
                .message(mensaje)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
