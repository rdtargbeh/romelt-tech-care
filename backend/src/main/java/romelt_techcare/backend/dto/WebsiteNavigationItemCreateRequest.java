package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.WebsiteNavigationDestinationType;
import romelt_techcare.backend.enums.WebsiteNavigationLocation;
import romelt_techcare.backend.enums.WebsiteNavigationTargetBehavior;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NAVIGATION ITEM CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries administrator input for creating a website navigation item.
 *
 * Destination:
 * At least one of websitePageId or destinationUrl must be supplied.
 * Detailed destination validation is enforced by the service.
 * ================================================================
 */
public record WebsiteNavigationItemCreateRequest(

        UUID websitePageId,

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

        WebsiteNavigationDestinationType destinationType,

        @Size(
                max = 1500,
                message = "Destination URL must not exceed 1500 characters."
        )
        String destinationUrl,

        WebsiteNavigationTargetBehavior targetBehavior,

        @Size(
                max = 100,
                message = "Icon key must not exceed 100 characters."
        )
        String iconKey,

        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder,

        Boolean isVisible
) {
}