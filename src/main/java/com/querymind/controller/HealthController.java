package com.querymind.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbHealthy = true;
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result == null || result != 1) {
                dbHealthy = false;
            }
        } catch (Exception e) {
            dbHealthy = false;
        }

        Map<String, Object> response = Map.of(
                "status", dbHealthy ? "UP" : "DEGRADED",
                "database", dbHealthy ? "CONNECTED" : "DISCONNECTED",
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.ok(response);
    }
}
