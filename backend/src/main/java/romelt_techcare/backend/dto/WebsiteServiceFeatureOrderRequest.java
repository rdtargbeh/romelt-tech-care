package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE FEATURE ORDER REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates the display order of one service feature.
 * ================================================================
 */
public record WebsiteServiceFeatureOrderRequest(

        @NotNull(message = "Display order is required.")
        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder
) {
}