package romelt_techcare.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import romelt_techcare.backend.enums.WebsitePageType;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC PAGE VERSION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns the current published page content and metadata required by
 * the React public website.
 *
 * Security:
 * Draft history, administrator identities, change summaries, archive
 * information, and optimistic-lock values are excluded.
 * ================================================================
 */
public record PublicWebsitePageVersionResponse(

        UUID websitePageId,

        String pageKey,

        String pageName,

        String routePath,

        WebsitePageType pageType,

        UUID pageVersionId,

        Integer versionNumber,

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

        Instant publishedAt
) {
}