package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsiteBillingInterval;
import romelt_techcare.backend.enums.WebsitePricingModel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC PRICING PLAN VERSION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns current published pricing-plan content to the public React
 * website.
 *
 * Security:
 * Administrator attribution, change summaries, archive information,
 * internal status, and optimistic-lock data are excluded.
 * ================================================================
 */
public record PublicWebsitePricingPlanVersionResponse(

        UUID pricingPlanId,

        String planCode,

        String planSlug,

        UUID pricingPlanVersionId,

        Integer versionNumber,

        String planName,

        String shortDescription,

        String fullDescription,

        WebsitePricingModel pricingModel,

        BigDecimal amount,

        String currencyCode,

        String pricePrefix,

        String priceSuffix,

        WebsiteBillingInterval billingInterval,

        String displayPrice,

        String callToActionLabel,

        String callToActionUrl,

        Integer displayOrder,

        Boolean isRecommended,

        Boolean isFeatured,

        Instant effectiveFrom,

        Instant effectiveUntil,

        Instant publishedAt
) {
}