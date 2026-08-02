package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.WebsitePageType;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC WEBSITE PAGE RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides the React website with the stable identity and current
 * published-version pointer for an active page.
 *
 * Actual page content will be supplied by the page-version module.
 * ================================================================
 */
public record PublicWebsitePageResponse(

        UUID websitePageId,

        String pageKey,

        String pageName,

        String routePath,

        WebsitePageType pageType,

        UUID publishedVersionId,

        Integer contentSchemaVersion
) {
}