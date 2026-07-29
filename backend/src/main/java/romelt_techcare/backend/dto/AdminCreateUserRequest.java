package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.AdminRole;

/**
 * ================================================================
 * ROMELT TECHCARE — CREATE ADMINISTRATOR USER REQUEST
 * ================================================================
 *
 * Purpose:
 * Represents the information submitted by a SUPER_ADMIN when creating
 * a new administrator or employee account.
 *
 * Responsibilities:
 * - Accepts administrator identity information.
 * - Accepts the role assigned to the new account.
 * - Accepts a temporary password.
 * - Requires the new account to change the temporary password after
 *   the first successful sign-in.
 *
 * Security rules:
 * - temporaryPassword must never be logged.
 * - temporaryPassword must never be returned in an API response.
 * - The service must validate and encode the password before saving.
 * - Only the resulting BCrypt hash may be persisted.
 *
 * Real-data integration:
 * Used by:
 * POST /api/v1/admin/users
 * ================================================================
 */
public record AdminCreateUserRequest(

        @NotBlank(message = "Email is required.")
        @Email(message = "A valid email address is required.")
        @Size(
                max = 254,
                message = "Email must not exceed 254 characters."
        )
        String email,

        @NotBlank(message = "First name is required.")
        @Size(
                min = 2,
                max = 100,
                message = "First name must contain between 2 and 100 characters."
        )
        String firstName,

        @NotBlank(message = "Last name is required.")
        @Size(
                min = 2,
                max = 100,
                message = "Last name must contain between 2 and 100 characters."
        )
        String lastName,

        @Size(
                max = 150,
                message = "Job title must not exceed 150 characters."
        )
        String jobTitle,

        @NotNull(message = "Administrator role is required.")
        AdminRole role,

        @NotBlank(message = "Temporary password is required.")
        @Size(
                min = 8,
                max = 72,
                message = "Temporary password must contain between 8 and 72 characters."
        )
        String temporaryPassword
) {
}