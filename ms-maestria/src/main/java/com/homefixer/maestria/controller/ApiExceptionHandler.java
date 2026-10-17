package com.homefixer.maestria.controller;

import com.homefixer.shared.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/** Devuelve el motivo de los ResponseStatusException en el cuerpo, para mostrarlo en el front. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handle(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(ErrorResponse.builder()
                .status(e.getStatusCode().value())
                .message(e.getReason())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
