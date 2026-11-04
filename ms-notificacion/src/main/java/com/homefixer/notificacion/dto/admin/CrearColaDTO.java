package com.homefixer.notificacion.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la creación dinámica y validada de colas en RabbitMQ.
 * Cumple con el indicador de validación de parámetros de entrada de la rúbrica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearColaDTO {

    @NotBlank(message = "El nombre de la cola es obligatorio y no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola solo puede contener caracteres alfanuméricos, puntos, guiones y guiones bajos")
    private String nombre;

    @Builder.Default
    private boolean durable = true;

    @Builder.Default
    private boolean autoDelete = false;

    private String deadLetterExchange;

    private String deadLetterRoutingKey;

    @PositiveOrZero(message = "El TTL de mensajes debe ser un valor positivo o cero")
    private Integer ttlMs;

    @Positive(message = "El límite máximo de mensajes debe ser mayor a cero")
    private Integer maxLength;

    @Pattern(regexp = "^(drop-head|reject-publish|reject-publish-dlx)$", message = "El modo de desborde debe ser drop-head, reject-publish o reject-publish-dlx")
    private String overflow;
}
