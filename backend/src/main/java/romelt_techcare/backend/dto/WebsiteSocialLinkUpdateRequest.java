package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SOCIAL LINK UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable state of one website social link.
 *
 * Update model:
 * This DTO is intended for a full PUT update.
 * ================================================================
 */
public record WebsiteSocialLinkUpdateRequest(

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

        @NotNull(
                message = "Display order is required."
        )
        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder,

        @NotNull(
                message = "Active status is required."
        )
        Boolean isActive
) {
}