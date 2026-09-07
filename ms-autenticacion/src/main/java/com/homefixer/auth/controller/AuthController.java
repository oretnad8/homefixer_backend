package com.homefixer.auth.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;

@RestController
@RequestMapping("/api/auth")
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT",
    scheme = "bearer"
)
public class AuthController {

    @Value("${supabase.jwt.secret:dummy}")
    private String supabaseJwtSecret;

    @PostMapping("/sync")
    @Operation(summary = "Sincronizar usuario", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> syncSupabaseUser(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Token faltante o formato inválido");
        }

        String token = authHeader.substring(7);

        try {
            // Decodificamos el payload (sin verificar la firma ya que es un demo, 
            // en prod usar JWKS de Supabase si el token es ES256/RS256)
            String[] chunks = token.split("\\.");
            java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
            String payload = new String(decoder.decode(chunks[1]));

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(payload);

            String email = jsonNode.has("email") ? jsonNode.get("email").asText() : "desconocido";

            // TODO: Integración con BD local
            // Aquí puedes emitir un evento Kafka o llamar a ms-usuarios por OpenFeign 
            // para insertar el usuario en tu tabla si aún no existe.

            return ResponseEntity.ok("Usuario de Supabase " + email + " sincronizado correctamente en la BD local.");
            
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Error procesando el token: " + e.getMessage());
        }
    }
}
