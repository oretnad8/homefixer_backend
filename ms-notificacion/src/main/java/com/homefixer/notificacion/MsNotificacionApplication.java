package com.homefixer.notificacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsNotificacionApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsNotificacionApplication.class, args);
    }
}
