package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN FEATURE ORDER REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates the display order of one pricing-plan feature.
 * ================================================================
 */
public record WebsitePricingPlanFeatureOrderRequest(

        @NotNull(message = "Display order is required.")
        @PositiveOrZero(
                message = "Display order must not be negative."
        )
        Integer displayOrder
) {
}