package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import romelt_techcare.backend.enums.WebsitePageVersionStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PAGE VERSION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing page-version information.
 * ================================================================
 */
public record WebsitePageVersionResponse(

        UUID pageVersionId,

        UUID websitePageId,

        String pageKey,

        String pageName,

        String routePath,

        Integer versionNumber,

        WebsitePageVersionStatus versionStatus,

        Integer contentSchemaVersion,

        JsonNode contentJson,

        String seoTitle,

        String seoDescription,

        String socialTitle,

        String socialDescription,

        PublicWebsiteMediaAssetResponse socialImage,

        String canonicalUrl,

        Boolean robotsIndex,

        Boolean robotsFollow,

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