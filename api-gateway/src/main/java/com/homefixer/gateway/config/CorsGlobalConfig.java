package com.homefixer.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Única fuente de CORS del sistema (la usa Spring Security en el gateway).
 * Los microservicios no deben agregar cabeceras CORS: se duplicarían.
 */
@Configuration
public class CorsGlobalConfig {

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Evitar el uso de "*" de forma global, pero permitiendo el túnel
        corsConfig.setAllowedOriginPatterns(List.of(frontendUrl, "https://homefixer.localfoto.cl", "https://*.localfoto.cl", "http://localhost:*"));

        // PATCH es necesario: asignar técnico, disponibilidad, especialidad...
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        corsConfig.setAllowedHeaders(List.of("*"));

        // El navegador cachea el preflight 1 h (menos OPTIONS al compartir ubicación)
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return source;
    }
}
