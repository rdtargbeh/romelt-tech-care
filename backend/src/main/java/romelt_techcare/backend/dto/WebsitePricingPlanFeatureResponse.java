package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN FEATURE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing pricing-plan feature
 * information.
 * ================================================================
 */
public record WebsitePricingPlanFeatureResponse(

        UUID pricingPlanFeatureId,

        UUID pricingPlanVersionId,

        UUID pricingPlanId,

        String planCode,

        String planSlug,

        Integer versionNumber,

        WebsitePricingPlanVersionStatus versionStatus,

        String featureText,

        String iconKey,

        Integer displayOrder,

        Boolean isActive,

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,

        String updatedByAdminUserDisplayName,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}