package com.example.inventoryservice.controller;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final Environment env;
    public HealthController(Environment env) { this.env = env; }

    @GetMapping("/api/health")
    public String health() {
        return "inventory-service OK on port " + env.getProperty("local.server.port");
    }
}
