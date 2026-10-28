package com.finalproject.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness only: no database, configuration or customer data is exposed. */
@RestController
public class HealthController {
    @GetMapping("/healthz")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
