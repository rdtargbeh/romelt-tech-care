package romelt_techcare.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ================================================================
 * ROMELT TECHCARE — HEALTH CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Provides a lightweight public endpoint used to verify that the
 * Spring Boot application is running and serving HTTP requests.
 *
 * Responsibilities:
 * - Confirm that the backend is available.
 * - Return a simple JSON response.
 * - Avoid exposing database credentials or infrastructure details.
 *
 * Endpoint:
 * GET /api/v1/public/health
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put("message", "Romelt TechCare backend is running.");
        response.put("application", "Romelt TechCare Backend");
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("path", "/api/v1/public/health");

        return ResponseEntity.ok(response);
    }
}