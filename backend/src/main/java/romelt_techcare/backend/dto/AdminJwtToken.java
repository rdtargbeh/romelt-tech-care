package romelt_techcare.backend.dto;

import java.time.Instant;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR JWT TOKEN RESULT
 * ================================================================
 *
 * Purpose:
 * Represents a newly generated administrator access token together
 * with its timing information.
 *
 * Responsibilities:
 * - Carries the signed JWT value.
 * - Carries the token issue timestamp.
 * - Carries the token expiration timestamp.
 * - Carries the token lifetime in seconds.
 *
 * Security rules:
 * - This DTO must never contain the JWT signing secret.
 * - This DTO must never contain a password or password hash.
 * - The token should be returned only after successful
 *   authentication.
 *
 * Real-data integration:
 * JwtService returns this record to AdminAuthenticationService,
 * which will place its values into AdminLoginResponse.
 * ================================================================
 */
public record AdminJwtToken(

        String value,

        Instant issuedAt,

        Instant expiresAt,

        long expiresInSeconds
) {

    /**
     * Validates the generated token result.
     */
    public AdminJwtToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT value must not be blank."
            );
        }

        if (issuedAt == null) {
            throw new IllegalArgumentException(
                    "JWT issue time must not be null."
            );
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException(
                    "JWT expiration time must not be null."
            );
        }

        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "JWT expiration must be after its issue time."
            );
        }

        if (expiresInSeconds <= 0) {
            throw new IllegalArgumentException(
                    "JWT lifetime must be greater than zero."
            );
        }
    }
}