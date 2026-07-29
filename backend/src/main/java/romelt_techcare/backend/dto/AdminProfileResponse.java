package romelt_techcare.backend.dto;

import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.enums.AdminRole;
import romelt_techcare.backend.enums.AdminStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN PROFILE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Represents safe administrator profile information returned to an
 * authenticated administration client.
 *
 * Responsibilities:
 * - Returns the administrator identity and account information.
 * - Returns role and account status.
 * - Returns password-change and login metadata.
 * - Prevents password hashes and security internals from being exposed.
 *
 * Security rules:
 * - passwordHash is never included.
 * - failedLoginAttempts is not exposed to normal clients.
 * - lockedUntil is not exposed through this profile response.
 * - Internal database security values remain server-side.
 *
 * Real-data integration:
 * Used by:
 * - POST /api/v1/admin/auth/login
 * - GET /api/v1/admin/auth/me
 * ================================================================
 */
public record AdminProfileResponse(

        UUID adminUserId,

        String email,

        String firstName,

        String lastName,

        String fullName,

        String jobTitle,

        AdminRole role,

        AdminStatus status,

        boolean mustChangePassword,

        Instant lastLoginAt,

        Instant passwordChangedAt,

        Instant createdAt,

        Instant updatedAt
) {

    /**
     * Creates a safe profile response from an administrator entity.
     *
     * @param adminUser persisted administrator
     * @return safe administrator profile response
     */
    public static AdminProfileResponse from(AdminUser adminUser) {
        if (adminUser == null) {
            throw new IllegalArgumentException(
                    "Admin user must not be null."
            );
        }

        return new AdminProfileResponse(
                adminUser.getAdminUserId(),
                adminUser.getEmail(),
                adminUser.getFirstName(),
                adminUser.getLastName(),
                adminUser.getFullName(),
                adminUser.getJobTitle(),
                adminUser.getRole(),
                adminUser.getStatus(),
                Boolean.TRUE.equals(adminUser.getMustChangePassword()),
                adminUser.getLastLoginAt(),
                adminUser.getPasswordChangedAt(),
                adminUser.getCreatedAt(),
                adminUser.getUpdatedAt()
        );
    }
}