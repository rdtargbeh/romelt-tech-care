package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN LOGIN REQUEST
 * ================================================================
 *
 * Purpose:
 * Represents the credentials submitted by an administrator when
 * signing in to the Romelt TechCare administration portal.
 *
 * Responsibilities:
 * - Accepts the administrator email address.
 * - Accepts the administrator's plain-text login password.
 * - Applies request-level validation before authentication begins.
 *
 * Security rules:
 * - The password exists only temporarily during the request.
 * - The password must never be logged.
 * - The password must never be returned in an API response.
 * - The password must never be stored directly in the database.
 * - Authentication compares this password against passwordHash using
 *   PasswordEncoder.matches(...).
 *
 * Real-data integration:
 * AdminAuthenticationController will receive this request and pass it
 * to AdminAuthenticationService.
 * ================================================================
 */
public record AdminLoginRequest(

        @NotBlank(message = "Email is required.")
        @Email(message = "A valid email address is required.")
        @Size(
                max = 254,
                message = "Email must not exceed 254 characters."
        )
        String email,

        @NotBlank(message = "Password is required.")
        @Size(
                min = 1,
                max = 72,
                message = "Password must not exceed 72 characters."
        )
        String password
) {
}