package romelt_techcare.backend.dto;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC PRICING PLAN SERVICE LINK RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns the stable public identity of a service included in the
 * current published version of a pricing plan.
 *
 * Actual service names, descriptions, images, pricing, and features
 * are provided by the published WebsiteServiceVersion content API.
 * ================================================================
 */
public record PublicWebsitePricingPlanVersionServiceResponse(

        UUID serviceId,

        String serviceCode,

        String serviceSlug,

        UUID publishedServiceVersionId
) {
}