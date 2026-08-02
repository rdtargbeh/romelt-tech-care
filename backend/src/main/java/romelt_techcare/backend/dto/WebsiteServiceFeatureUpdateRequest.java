package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE FEATURE UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries the complete editable state of one service feature.
 *
 * Rules:
 * Only features belonging to a DRAFT service version may be updated.
 * ================================================================
 */
public record WebsiteServiceFeatureUpdateRequest(

        @NotBlank(message = "Feature text is required.")
        @Size(
                max = 500,
                message = "Feature text must not exceed 500 characters."
        )
        String featureText,

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

        @NotNull(message = "Active status is required.")
        Boolean isActive
) {
}