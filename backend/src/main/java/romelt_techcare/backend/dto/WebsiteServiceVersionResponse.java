package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsiteServiceStatus;
import romelt_techcare.backend.enums.WebsiteServiceVersionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE VERSION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing service-version information.
 * ================================================================
 */
public record WebsiteServiceVersionResponse(

        UUID serviceVersionId,

        UUID serviceId,

        String serviceCode,

        String serviceSlug,

        WebsiteServiceStatus serviceStatus,

        Integer versionNumber,

        WebsiteServiceVersionStatus versionStatus,

        String serviceName,

        String shortDescription,

        String fullDescription,

        String iconKey,

        PublicWebsiteMediaAssetResponse cardImage,

        PublicWebsiteMediaAssetResponse heroImage,

        BigDecimal startingPrice,

        String currencyCode,

        String priceUnitLabel,

        Integer displayOrder,

        Boolean isFeatured,

        Boolean isBookable,

        Boolean isPublic,

        String seoTitle,

        String seoDescription,

        Instant effectiveFrom,

        Instant effectiveUntil,

        Boolean currentlyEffective,

        Boolean publiclyAvailable,

        Boolean publiclyBookable,

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