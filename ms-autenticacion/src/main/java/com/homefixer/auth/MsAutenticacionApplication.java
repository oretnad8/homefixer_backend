package com.homefixer.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsAutenticacionApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsAutenticacionApplication.class, args);
    }
}
