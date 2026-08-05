package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMINISTRATOR PASSWORD RESET REQUEST
 * ================================================================
 *
 * Purpose:
 * Allows a SUPER_ADMIN to assign a temporary password to another
 * administrator account.
 *
 * Responsibilities:
 * - Accepts a new temporary password.
 * - Requires the affected administrator to replace the password after
 *   the next successful sign-in.
 * - Clears failed-login attempts and temporary lockout information.
 *
 * Security rules:
 * - temporaryPassword must never be logged.
 * - temporaryPassword must never be returned.
 * - The service must encode the password before persistence.
 * - A SUPER_ADMIN may not use this endpoint to reset their own
 *   password; the authenticated password-change endpoint must be used.
 *
 * Real-data integration:
 * Used by:
 * POST /api/v1/admin/users/{adminUserId}/reset-password
 * ================================================================
 */
public record AdminResetPasswordRequest(

        @NotBlank(message = "Temporary password is required.")
        @Size(
                min = 8,
                max = 72,
                message = "Temporary password must contain between 8 and 72 characters."
        )
        String temporaryPassword
) {
}