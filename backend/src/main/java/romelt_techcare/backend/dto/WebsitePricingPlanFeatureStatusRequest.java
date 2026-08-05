package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN FEATURE STATUS REQUEST
 * ================================================================
 *
 * Purpose:
 * Updates whether a pricing-plan feature is active.
 * ================================================================
 */
public record WebsitePricingPlanFeatureStatusRequest(

        @NotNull(message = "Active status is required.")
        Boolean isActive
) {
}