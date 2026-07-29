package romelt_techcare.backend.dto;

import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.enums.AdminRole;
import romelt_techcare.backend.enums.AdminStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR USER RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns safe administrator account information to authorized
 * SUPER_ADMIN users.
 *
 * Responsibilities:
 * - Returns administrator identity and role information.
 * - Returns status and security-state information.
 * - Returns login and password-change timestamps.
 * - Excludes password hashes and other secrets.
 *
 * Security rules:
 * - passwordHash is never returned.
 * - JWT values are never returned.
 * - Plain-text passwords are never returned.
 *
 * Real-data integration:
 * Used by the administrator user-management endpoints.
 * ================================================================
 */
public record AdminUserResponse(

        UUID adminUserId,

        String email,

        String firstName,

        String lastName,

        String fullName,

        String jobTitle,

        AdminRole role,

        AdminStatus status,

        boolean mustChangePassword,

        int failedLoginAttempts,

        Instant lockedUntil,

        Instant lastLoginAt,

        Instant passwordChangedAt,

        Instant createdAt,

        Instant updatedAt
) {

    public static AdminUserResponse from(
            AdminUser administrator
    ) {
        if (administrator == null) {
            throw new IllegalArgumentException(
                    "Administrator must not be null."
            );
        }

        return new AdminUserResponse(
                administrator.getAdminUserId(),
                administrator.getEmail(),
                administrator.getFirstName(),
                administrator.getLastName(),
                administrator.getFullName(),
                administrator.getJobTitle(),
                administrator.getRole(),
                administrator.getStatus(),
                Boolean.TRUE.equals(
                        administrator.getMustChangePassword()
                ),
                administrator.getFailedLoginAttempts() == null
                        ? 0
                        : administrator.getFailedLoginAttempts(),
                administrator.getLockedUntil(),
                administrator.getLastLoginAt(),
                administrator.getPasswordChangedAt(),
                administrator.getCreatedAt(),
                administrator.getUpdatedAt()
        );
    }
}