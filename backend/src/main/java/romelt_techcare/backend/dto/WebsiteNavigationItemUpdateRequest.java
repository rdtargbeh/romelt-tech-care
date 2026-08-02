package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsiteNavigationDestinationType;
import romelt_techcare.backend.enums.WebsiteNavigationLocation;
import romelt_techcare.backend.enums.WebsiteNavigationTargetBehavior;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NAVIGATION ITEM UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable state of one navigation item.
 *
 * Update model:
 * This request is intended for a full PUT operation.
 * ================================================================
 */
public record WebsiteNavigationItemUpdateRequest(

        UUID websitePageId,

        @NotNull(message = "Navigation location is required.")
        WebsiteNavigationLocation navigationLocation,

        @NotBlank(message = "Navigation item key is required.")
        @Size(
                max = 120,
                message = "Navigation item key must not exceed 120 characters."
        )
        String itemKey,

        @NotBlank(message = "Navigation label is required.")
        @Size(
                max = 180,
                message = "Navigation label must not exceed 180 characters."
        )
        String label,

        @NotNull(message = "Destination type is required.")
        WebsiteNavigationDestinationType destinationType,

        @Size(
                max = 1500,
                message = "Destination URL must not exceed 1500 characters."
        )
        String destinationUrl,

        @NotNull(message = "Target behavior is required.")
        WebsiteNavigationTargetBehavior targetBehavior,

        @Size(
                max = 100,
                message = "Icon key must not exceed 100 characters."
        )
        String iconKey,

        @NotNull(message = "Display order is required.")
        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder,

        @NotNull(message = "Visibility status is required.")
        Boolean isVisible
) {
}