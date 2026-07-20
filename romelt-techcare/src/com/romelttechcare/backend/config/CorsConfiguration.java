package com.romelttechcare.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — CORS CONFIGURATION
 * ================================================================
 *
 * Purpose:
 * Allows the approved React frontend to call public backend APIs.
 *
 * Responsibilities:
 * - Reads the approved frontend origin from application properties.
 * - Restricts HTTP methods and request headers.
 * - Applies CORS rules to all API endpoints.
 *
 * Production:
 * app.cors.allowed-origin must contain the deployed website origin.
 * Wildcard origins must not be used with authenticated requests.
 * ================================================================
 */
@Configuration
public class CorsConfiguration {

    @Value(
            "${app.cors.allowed-origin:http://localhost:5173}"
    )
    private String allowedOrigin;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration =
                new org.springframework.web.cors.CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(allowedOrigin)
        );

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin"
                )
        );

        configuration.setExposedHeaders(
                List.of(
                        "Location"
                )
        );

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/api/**",
                configuration
        );

        return source;
    }
}