package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus;
import romelt_techcare.backend.enums.WebsiteServiceStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN SERVICE LINK RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns administrator-facing information for one relationship
 * between a pricing-plan version and a website service.
 * ================================================================
 */
public record WebsitePricingPlanVersionServiceResponse(

        UUID pricingPlanVersionId,

        UUID pricingPlanId,

        String planCode,

        String planSlug,

        Integer pricingPlanVersionNumber,

        WebsitePricingPlanVersionStatus pricingPlanVersionStatus,

        UUID serviceId,

        String serviceCode,

        String serviceSlug,

        WebsiteServiceStatus serviceStatus,

        UUID serviceDraftVersionId,

        UUID servicePublishedVersionId,

        Boolean serviceDeleted,

        Boolean servicePubliclyAvailable,

        Instant createdAt
) {
}