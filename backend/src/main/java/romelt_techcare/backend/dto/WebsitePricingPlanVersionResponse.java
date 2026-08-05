package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsiteBillingInterval;
import romelt_techcare.backend.enums.WebsitePricingModel;
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;
import romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing pricing-plan version
 * information.
 * ================================================================
 */
public record WebsitePricingPlanVersionResponse(

        UUID pricingPlanVersionId,

        UUID pricingPlanId,

        String planCode,

        String planSlug,

        WebsitePricingPlanStatus planStatus,

        Integer versionNumber,

        WebsitePricingPlanVersionStatus versionStatus,

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

        Boolean isPublic,

        Instant effectiveFrom,

        Instant effectiveUntil,

        Boolean currentlyEffective,

        Boolean publiclyAvailable,

        String changeSummary,

        Instant publishedAt,

        Instant archivedAt,

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,

        String updatedByAdminUserDisplayName,

        UUID publishedByAdminUserId,

        String publishedByAdminUserDisplayName,

        UUID archivedByAdminUserId,

        String archivedByAdminUserDisplayName,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}