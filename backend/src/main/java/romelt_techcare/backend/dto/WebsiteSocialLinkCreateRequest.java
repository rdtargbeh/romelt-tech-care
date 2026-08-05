package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SOCIAL LINK CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries validated administrator input for creating a public social
 * or external-profile link.
 *
 * Responsibilities:
 * - Accepts the platform identifier.
 * - Accepts an optional display label and frontend icon key.
 * - Accepts the public profile URL.
 * - Accepts display ordering and active status.
 *
 * Protected fields:
 * Identifiers, administrator ownership, timestamps, and optimistic-lock
 * values are assigned by the backend.
 * ================================================================
 */
public record WebsiteSocialLinkCreateRequest(

        @NotBlank(
                message = "Platform is required."
        )
        @Size(
                max = 50,
                message = "Platform must not exceed 50 characters."
        )
        String platform,

        @Size(
                max = 100,
                message = "Label must not exceed 100 characters."
        )
        String label,

        @NotBlank(
                message = "Profile URL is required."
        )
        @Size(
                max = 1000,
                message = "Profile URL must not exceed 1000 characters."
        )
        String profileUrl,

        @Size(
                max = 100,
                message = "Icon key must not exceed 100 characters."
        )
        String iconKey,

        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder,

        Boolean isActive
) {
}