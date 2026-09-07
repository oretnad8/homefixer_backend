package com.homefixer.auth.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String nombre;
    private String email;
    private String telefono;
    private String password;
    private String tipo; // CLIENTE o TECNICO
}
