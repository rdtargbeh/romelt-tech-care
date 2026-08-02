package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsitePricingPlanStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing information for a stable
 * website pricing-plan identity.
 * ================================================================
 */
public record WebsitePricingPlanResponse(

        UUID pricingPlanId,

        String planCode,

        String planSlug,

        UUID draftVersionId,

        UUID publishedVersionId,

        WebsitePricingPlanStatus planStatus,

        Boolean deleted,

        Boolean publiclyAvailable,

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,

        String updatedByAdminUserDisplayName,

        UUID deletedByAdminUserId,

        String deletedByAdminUserDisplayName,

        Instant deletedAt,

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}