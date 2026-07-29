package romelt_techcare.backend.dto;

import java.time.Instant;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN LOGIN RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns the JWT access token and authenticated administrator profile
 * after a successful login.
 *
 * Responsibilities:
 * - Returns the signed JWT access token.
 * - Identifies the token type.
 * - Returns the token expiration timestamp.
 * - Returns the authenticated administrator profile.
 * - Communicates whether the administrator must change the password.
 *
 * Security rules:
 * - The plain-text password is never returned.
 * - The stored password hash is never returned.
 * - The JWT secret is never returned.
 * - Only a signed access token is exposed to the client.
 *
 * Real-data integration:
 * Returned by:
 * POST /api/v1/admin/auth/login
 * ================================================================
 */
public record AdminLoginResponse(

        String accessToken,

        String tokenType,

        Instant expiresAt,

        long expiresInSeconds,

        boolean mustChangePassword,

        AdminProfileResponse administrator
) {

    /**
     * Standard token type used by the Authorization header.
     */
    public static final String BEARER_TOKEN_TYPE = "Bearer";

    /**
     * Creates a successful login response.
     *
     * @param accessToken signed JWT access token
     * @param expiresAt token expiration timestamp
     * @param expiresInSeconds token lifetime in seconds
     * @param administrator authenticated administrator profile
     * @return login response
     */
    public static AdminLoginResponse success(
            String accessToken,
            Instant expiresAt,
            long expiresInSeconds,
            AdminProfileResponse administrator
    ) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Access token must not be blank."
            );
        }

        if (expiresAt == null) {
            throw new IllegalArgumentException(
                    "Token expiration must not be null."
            );
        }

        if (expiresInSeconds <= 0) {
            throw new IllegalArgumentException(
                    "Token lifetime must be greater than zero."
            );
        }

        if (administrator == null) {
            throw new IllegalArgumentException(
                    "Administrator profile must not be null."
            );
        }

        return new AdminLoginResponse(
                accessToken,
                BEARER_TOKEN_TYPE,
                expiresAt,
                expiresInSeconds,
                administrator.mustChangePassword(),
                administrator
        );
    }
}