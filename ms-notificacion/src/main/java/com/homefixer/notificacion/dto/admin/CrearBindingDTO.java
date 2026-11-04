package com.homefixer.notificacion.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la vinculación (binding) entre colas y exchanges.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearBindingDTO {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    private String cola;

    @NotBlank(message = "El nombre del exchange es obligatorio")
    private String exchange;

    @NotBlank(message = "La routing key es obligatoria")
    private String routingKey;
}
