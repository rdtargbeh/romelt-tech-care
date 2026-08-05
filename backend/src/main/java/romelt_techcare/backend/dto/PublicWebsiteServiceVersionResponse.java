package romelt_techcare.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC SERVICE VERSION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns the current published content for an active website service.
 *
 * Security:
 * Administrator attribution, change summaries, archive information,
 * draft metadata, and optimistic-lock values are excluded.
 * ================================================================
 */
public record PublicWebsiteServiceVersionResponse(

        UUID serviceId,

        String serviceCode,

        String serviceSlug,

        UUID serviceVersionId,

        Integer versionNumber,

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

        String seoTitle,

        String seoDescription,

        Instant effectiveFrom,

        Instant effectiveUntil,

        Instant publishedAt
) {
}