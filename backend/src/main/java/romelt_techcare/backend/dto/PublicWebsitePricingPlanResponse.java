package romelt_techcare.backend.dto;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC PRICING PLAN RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns the stable identity and current published-version pointer
 * of an active public website pricing plan.
 *
 * Actual pricing-plan content will be returned by the future
 * WebsitePricingPlanVersion public API.
 * ================================================================
 */
public record PublicWebsitePricingPlanResponse(

        UUID pricingPlanId,

        String planCode,

        String planSlug,

        UUID publishedVersionId
) {
}