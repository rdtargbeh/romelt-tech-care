package romelt_techcare.backend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * ================================================================
 * ROMELT TECHCARE — JWT CONFIGURATION PROPERTIES
 * ================================================================
 *
 * Purpose:
 * Provides centralized JWT configuration for administrator
 * authentication using Spring Security's native JWT support.
 *
 * Responsibilities:
 * - Loads the JWT signing secret.
 * - Defines the token issuer.
 * - Defines the intended audience.
 * - Defines administrator access-token lifetime.
 * - Validates required configuration during application startup.
 *
 * Configuration Prefix:
 *
 *     romelt.security.jwt
 *
 * Example:
 *
 * romelt.security.jwt.secret=${ROMELT_JWT_SECRET}
 * romelt.security.jwt.issuer=${ROMELT_JWT_ISSUER:romelt-techcare-backend}
 * romelt.security.jwt.audience=${ROMELT_JWT_AUDIENCE:romelt-techcare-admin}
 * romelt.security.jwt.access-token-lifetime=PT8H
 *
 * Security Rules:
 * - Secret should always come from an environment variable.
 * - Secret must never be logged.
 * - Secret must contain at least 32 UTF-8 bytes.
 * - Passwords are never stored inside JWT tokens.
 *
 * Real-data integration:
 * JwtService uses these values to generate and validate signed
 * administrator access tokens.
 * ================================================================
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "romelt.security.jwt")
public class JwtConfigurationProperties {

    /**
     * Secret used for HMAC signing.
     */
    @NotBlank
    private String secret;

    /**
     * JWT issuer.
     */
    @NotBlank
    private String issuer = "romelt-techcare-backend";

    /**
     * Intended audience.
     */
    @NotBlank
    private String audience = "romelt-techcare-admin";

    /**
     * Administrator access-token lifetime.
     */
    @NotNull
    private Duration accessTokenLifetime = Duration.ofHours(8);

}