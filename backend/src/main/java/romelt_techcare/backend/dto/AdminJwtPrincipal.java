package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.AdminRole;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR JWT PRINCIPAL
 * ================================================================
 *
 * Purpose:
 * Represents the authenticated administrator information extracted
 * from a valid JWT access token.
 *
 * Responsibilities:
 * - Carries the administrator database identifier.
 * - Carries the administrator email.
 * - Carries the administrator role.
 * - Carries the JWT identifier.
 * - Carries token issue and expiration timestamps.
 *
 * Security rules:
 * - A principal is created only from a verified token.
 * - A token must pass signature, issuer, audience, and expiration
 *   validation before this record is returned.
 * - No password information is included.
 *
 * Real-data integration:
 * The future JWT authentication filter will use this record to create
 * the Spring Security Authentication object.
 * ================================================================
 */
public record AdminJwtPrincipal(

        UUID adminUserId,

        String email,

        AdminRole role,

        String tokenId,

        Instant issuedAt,

        Instant expiresAt
) {

    /**
     * Validates the extracted token principal.
     */
    public AdminJwtPrincipal {
        if (adminUserId == null) {
            throw new IllegalArgumentException(
                    "Administrator ID must not be null."
            );
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Administrator email must not be blank."
            );
        }

        if (role == null) {
            throw new IllegalArgumentException(
                    "Administrator role must not be null."
            );
        }

        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException(
                    "Token ID must not be blank."
            );
        }

        if (issuedAt == null || expiresAt == null) {
            throw new IllegalArgumentException(
                    "Token timestamps must not be null."
            );
        }
    }
}