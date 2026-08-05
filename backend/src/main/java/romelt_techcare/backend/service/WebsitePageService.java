package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsitePage;
import romelt_techcare.backend.enums.WebsitePageType;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines business operations for stable website-page identities.
 *
 * Responsibilities:
 * - Creates and updates fixed React page records.
 * - Retrieves pages by identifier, key, and route.
 * - Searches pages using administrator filters.
 * - Assigns draft and published version pointers.
 * - Activates and deactivates pages.
 * - Soft-deletes and restores pages.
 * - Retrieves active public page identities.
 * ================================================================
 */
public interface WebsitePageService {

    WebsitePage createWebsitePage(
            WebsitePage websitePage,
            UUID administratorId
    );

    WebsitePage updateWebsitePage(
            UUID websitePageId,
            WebsitePage requestedUpdate,
            UUID administratorId
    );

    WebsitePage getWebsitePage(
            UUID websitePageId
    );

    WebsitePage getWebsitePageByKey(
            String pageKey
    );

    WebsitePage getWebsitePageByRoute(
            String routePath
    );

    Page<WebsitePage> searchWebsitePages(
            String keyword,
            WebsitePageType pageType,
            Boolean isSystemPage,
            Boolean isActive,
            Pageable pageable
    );

    Page<WebsitePage> getDeletedWebsitePages(
            Pageable pageable
    );

    WebsitePage assignDraftVersion(
            UUID websitePageId,
            UUID pageVersionId,
            UUID administratorId
    );

    WebsitePage clearDraftVersion(
            UUID websitePageId,
            UUID administratorId
    );

    WebsitePage assignPublishedVersion(
            UUID websitePageId,
            UUID pageVersionId,
            UUID administratorId
    );

    WebsitePage clearPublishedVersion(
            UUID websitePageId,
            UUID administratorId
    );

    WebsitePage updateWebsitePageStatus(
            UUID websitePageId,
            boolean isActive,
            UUID administratorId
    );

    WebsitePage activateWebsitePage(
            UUID websitePageId,
            UUID administratorId
    );

    WebsitePage deactivateWebsitePage(
            UUID websitePageId,
            UUID administratorId
    );

    void deleteWebsitePage(
            UUID websitePageId,
            UUID administratorId
    );

    WebsitePage restoreWebsitePage(
            UUID websitePageId,
            UUID administratorId
    );

    WebsitePage getPublicWebsitePageByKey(
            String pageKey
    );

    WebsitePage getPublicWebsitePageByRoute(
            String routePath
    );

    List<WebsitePage> getPublicWebsitePages();

    long countActiveWebsitePages();

    long countDeletedWebsitePages();
}