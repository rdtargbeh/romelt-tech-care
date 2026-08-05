package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsitePageVersion;
import romelt_techcare.backend.enums.WebsitePageVersionStatus;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE VERSION SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines draft, publication, archive, and history operations for
 * versioned website-page content.
 *
 * Responsibilities:
 * - Creates a new page draft.
 * - Creates a draft by copying the current published version.
 * - Updates the current draft.
 * - Publishes a draft transactionally.
 * - Archives the previous published version.
 * - Updates the owning WebsitePage version pointers.
 * - Retrieves administrator version history.
 * - Retrieves public published content.
 * - Synchronizes explicit social-image media usage.
 * ================================================================
 */
public interface WebsitePageVersionService {

    WebsitePageVersion createDraft(
            UUID websitePageId,
            WebsitePageVersion requestedDraft,
            UUID administratorId
    );

    WebsitePageVersion createDraftFromPublishedVersion(
            UUID websitePageId,
            String changeSummary,
            UUID administratorId
    );

    WebsitePageVersion updateDraft(
            UUID pageVersionId,
            WebsitePageVersion requestedUpdate,
            UUID administratorId
    );

    WebsitePageVersion getPageVersion(
            UUID pageVersionId
    );

    WebsitePageVersion getCurrentDraft(
            UUID websitePageId
    );

    WebsitePageVersion getCurrentPublishedVersion(
            UUID websitePageId
    );

    Page<WebsitePageVersion> getVersionHistory(
            UUID websitePageId,
            WebsitePageVersionStatus versionStatus,
            Pageable pageable
    );

    WebsitePageVersion publishDraft(
            UUID pageVersionId,
            UUID administratorId
    );

    WebsitePageVersion archiveVersion(
            UUID pageVersionId,
            UUID administratorId
    );

    void deleteDraft(
            UUID pageVersionId,
            UUID administratorId
    );

    WebsitePageVersion getPublicPageByKey(
            String pageKey
    );

    WebsitePageVersion getPublicPageByRoute(
            String routePath
    );
}