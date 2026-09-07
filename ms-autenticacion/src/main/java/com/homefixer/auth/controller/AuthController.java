package com.homefixer.auth.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Value("${supabase.jwt.secret}")
    private String supabaseJwtSecret;

    @PostMapping("/sync")
    public ResponseEntity<String> syncSupabaseUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Token faltante o formato inválido");
        }

        String token = authHeader.substring(7);

        try {
            // Supabase firma sus JWT localmente con HS256 y este secreto
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(supabaseJwtSecret.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Extraemos los Claims por defecto emitidos por Supabase
            String supabaseUserId = claims.getSubject();
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class); // Usualmente es 'authenticated'

            // TODO: Integración con BD local
            // Aquí puedes emitir un evento Kafka o llamar a ms-usuarios por OpenFeign 
            // para insertar el usuario en tu tabla si aún no existe.
            // Ejemplo: usuarioClient.crearUsuarioLocal(supabaseUserId, email);

            return ResponseEntity.ok("Usuario de Supabase " + email + " sincronizado correctamente en la BD local.");
            
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Firma del token inválida o expirada: " + e.getMessage());
        }
    }
}
