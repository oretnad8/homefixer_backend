package com.homefixer.kafkaadmin;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Microservicio administrador de Kafka (EP5): gestiona tópicos (crear, listar, modificar configuraciones,
 * ampliar particiones, eliminar), inspecciona el clúster, administra consumer groups (estado, lag,
 * reset de offsets, eliminación) y audita los Dead Letter Topics. Expuesto por el api-gateway
 * en /api/kafka/admin/** solo para ROLE_ADMIN.
 */
@OpenAPIDefinition(info = @Info(title = "HomeFixer · Kafka Admin", version = "1.0"),
        security = {@SecurityRequirement(name = "bearerAuth")})
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@SpringBootApplication
@EnableDiscoveryClient
public class MsKafkaAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsKafkaAdminApplication.class, args);
    }
}
