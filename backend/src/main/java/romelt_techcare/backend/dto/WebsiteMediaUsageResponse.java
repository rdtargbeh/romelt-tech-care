package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;
import romelt_techcare.backend.enums.WebsiteMediaUsageResourceType;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA USAGE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides administrator-safe information describing where one
 * website media asset is used.
 *
 * Responsibilities:
 * - Identifies the usage record.
 * - Identifies the referenced media asset.
 * - Identifies the owning CMS resource.
 * - Identifies the exact field or placement using the media.
 * - Provides enough media information for an administrator usage list.
 *
 * Security:
 * This response does not expose internal storage keys, file paths,
 * administrator authentication information, or unrelated entity data.
 *
 * Write policy:
 * Media usage records are system-managed. This DTO is read-only.
 * ================================================================
 */
public record WebsiteMediaUsageResponse(

        UUID mediaUsageId,

        UUID mediaAssetId,

        String mediaAssetKey,

        String mediaTitle,

        String originalFileName,

        String publicUrl,

        String mimeType,

        WebsiteMediaAssetStatus mediaAssetStatus,

        Boolean mediaAssetPublic,

        WebsiteMediaUsageResourceType resourceType,

        UUID resourceId,

        String usageField,

        String usageDescription,

        Instant createdAt
) {
}