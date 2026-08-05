package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CHANGE PASSWORD REQUEST
 * ================================================================
 *
 * Purpose:
 * Allows an authenticated administrator to replace the current
 * password with a new memorable password.
 *
 * Responsibilities:
 * - Accepts the administrator's current password.
 * - Accepts the desired new password.
 * - Requires confirmation of the new password.
 * - Applies basic request-size validation.
 *
 * Security rules:
 * - Password values must never be logged.
 * - Password values must never be returned in an API response.
 * - The current password must be verified using PasswordEncoder.
 * - The new password must be encoded before persistence.
 * - Only the encoded password hash is stored in password_hash.
 * - The service layer must verify that newPassword and
 *   confirmNewPassword match.
 * - The service layer must apply complete password-strength rules.
 *
 * Real-data integration:
 * This request will be used by the administrator password-change
 * endpoint after JWT authentication is active.
 * ================================================================
 */
public record AdminChangePasswordRequest(

        @NotBlank(message = "Current password is required.")
        @Size(
                max = 72,
                message = "Current password must not exceed 72 characters."
        )
        String currentPassword,

        @NotBlank(message = "New password is required.")
        @Size(
                min = 8,
                max = 72,
                message = "New password must contain between 8 and 72 characters."
        )
        String newPassword,

        @NotBlank(message = "Password confirmation is required.")
        @Size(
                min = 8,
                max = 72,
                message = "Password confirmation must contain between 8 and 72 characters."
        )
        String confirmNewPassword
) {

    /**
     * Returns true when the new password and confirmation match.
     */
    public boolean passwordsMatch() {
        return newPassword != null
                && newPassword.equals(confirmNewPassword);
    }
}