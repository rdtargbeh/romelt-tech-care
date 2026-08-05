package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsitePageType;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns complete administrator-facing information for a stable
 * website page.
 * ================================================================
 */
public record WebsitePageResponse(

        UUID websitePageId,

        String pageKey,

        String pageName,

        String routePath,

        WebsitePageType pageType,

        UUID draftVersionId,

        UUID publishedVersionId,

        Integer contentSchemaVersion,

        Boolean isSystemPage,

        Boolean isActive,

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