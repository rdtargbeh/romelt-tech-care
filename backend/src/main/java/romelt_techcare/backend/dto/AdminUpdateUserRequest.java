package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.AdminRole;

/**
 * ================================================================
 * ROMELT TECHCARE — UPDATE ADMINISTRATOR USER REQUEST
 * ================================================================
 *
 * Purpose:
 * Represents editable profile and authorization information for an
 * existing administrator account.
 *
 * Responsibilities:
 * - Updates administrator identity information.
 * - Updates the administrator email address.
 * - Updates the administrator job title.
 * - Updates the assigned administrator role.
 *
 * Security rules:
 * - Account status is managed through the dedicated status endpoint.
 * - Passwords are managed through password-change or reset endpoints.
 * - The final active SUPER_ADMIN may not be demoted.
 *
 * Real-data integration:
 * Used by:
 * PUT /api/v1/admin/users/{adminUserId}
 * ================================================================
 */
public record AdminUpdateUserRequest(

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
        AdminRole role
) {
}