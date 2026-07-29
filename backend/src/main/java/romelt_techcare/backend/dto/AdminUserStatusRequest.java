package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;
import romelt_techcare.backend.enums.AdminStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR STATUS REQUEST
 * ================================================================
 *
 * Purpose:
 * Changes the operational status of an administrator account.
 *
 * Supported statuses:
 * - ACTIVE
 * - INACTIVE
 * - LOCKED
 *
 * Security rules:
 * - A SUPER_ADMIN may not disable or lock their own account.
 * - The final active SUPER_ADMIN may not be disabled or locked.
 * - Setting ACTIVE also clears failed-login and lockout information.
 *
 * Real-data integration:
 * Used by:
 * PATCH /api/v1/admin/users/{adminUserId}/status
 * ================================================================
 */
public record AdminUserStatusRequest(

        @NotNull(message = "Administrator status is required.")
        AdminStatus status
) {
}