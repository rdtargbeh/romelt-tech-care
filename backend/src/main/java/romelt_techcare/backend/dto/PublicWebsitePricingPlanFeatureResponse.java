package romelt_techcare.backend.dto;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC PRICING PLAN FEATURE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns one active feature belonging to the current published
 * pricing-plan version.
 *
 * Security:
 * Administrator attribution, timestamps, version status, and
 * optimistic-lock information are excluded.
 * ================================================================
 */
public record PublicWebsitePricingPlanFeatureResponse(

        UUID pricingPlanFeatureId,

        String featureText,

        String iconKey,

        Integer displayOrder
) {
}