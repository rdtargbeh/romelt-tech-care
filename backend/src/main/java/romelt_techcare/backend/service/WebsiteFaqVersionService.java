package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsiteFaqVersion;
import romelt_techcare.backend.enums.WebsiteFaqVersionStatus;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — FAQ VERSION SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines draft, publication, archival, history, and public retrieval
 * operations for versioned website FAQ content.
 *
 * Responsibilities:
 * - Creates new FAQ drafts.
 * - Creates drafts from published FAQ versions.
 * - Updates draft question-and-answer content.
 * - Publishes drafts transactionally.
 * - Archives previous published versions.
 * - Synchronizes stable FAQ version pointers.
 * - Retrieves administrator version history.
 * - Retrieves public FAQs by key, category, and featured status.
 * ================================================================
 */
public interface WebsiteFaqVersionService {

    WebsiteFaqVersion createDraft(
            UUID faqId,
            WebsiteFaqVersion requestedDraft,
            UUID administratorId
    );

    WebsiteFaqVersion createDraftFromPublishedVersion(
            UUID faqId,
            String changeSummary,
            UUID administratorId
    );

    WebsiteFaqVersion updateDraft(
            UUID faqVersionId,
            WebsiteFaqVersion requestedUpdate,
            UUID administratorId
    );

    WebsiteFaqVersion getFaqVersion(
            UUID faqVersionId
    );

    WebsiteFaqVersion getCurrentDraft(
            UUID faqId
    );

    WebsiteFaqVersion getCurrentPublishedVersion(
            UUID faqId
    );

    Page<WebsiteFaqVersion> getVersionHistory(
            UUID faqId,
            WebsiteFaqVersionStatus versionStatus,
            Pageable pageable
    );

    WebsiteFaqVersion publishDraft(
            UUID faqVersionId,
            UUID administratorId
    );

    WebsiteFaqVersion archiveVersion(
            UUID faqVersionId,
            UUID administratorId
    );

    void deleteDraft(
            UUID faqVersionId,
            UUID administratorId
    );

    WebsiteFaqVersion getPublicFaqByKey(
            String faqKey
    );

    List<WebsiteFaqVersion> getPublicFaqs();

    List<WebsiteFaqVersion> getFeaturedPublicFaqs();

    List<WebsiteFaqVersion> getPublicFaqsByCategory(
            String faqCategory
    );

    List<String> getPublicFaqCategories();
}