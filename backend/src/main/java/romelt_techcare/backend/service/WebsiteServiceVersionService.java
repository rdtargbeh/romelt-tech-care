package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsiteServiceVersion;
import romelt_techcare.backend.enums.WebsiteServiceVersionStatus;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE VERSION SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines draft, publication, archive, history, and public retrieval
 * operations for versioned website-service content.
 *
 * Responsibilities:
 * - Creates a new service draft.
 * - Creates a draft by copying the published version.
 * - Updates the current draft.
 * - Publishes a draft transactionally.
 * - Archives the previously published version.
 * - Synchronizes WebsiteService draft and published pointers.
 * - Retrieves administrator version history.
 * - Retrieves public, featured, and bookable services.
 * - Synchronizes card and hero media usage records.
 * ================================================================
 */
public interface WebsiteServiceVersionService {

    WebsiteServiceVersion createDraft(
            UUID serviceId,
            WebsiteServiceVersion requestedDraft,
            UUID administratorId
    );

    WebsiteServiceVersion createDraftFromPublishedVersion(
            UUID serviceId,
            String changeSummary,
            UUID administratorId
    );

    WebsiteServiceVersion updateDraft(
            UUID serviceVersionId,
            WebsiteServiceVersion requestedUpdate,
            UUID administratorId
    );

    WebsiteServiceVersion getServiceVersion(
            UUID serviceVersionId
    );

    WebsiteServiceVersion getCurrentDraft(
            UUID serviceId
    );

    WebsiteServiceVersion getCurrentPublishedVersion(
            UUID serviceId
    );

    Page<WebsiteServiceVersion> getVersionHistory(
            UUID serviceId,
            WebsiteServiceVersionStatus versionStatus,
            Pageable pageable
    );

    WebsiteServiceVersion publishDraft(
            UUID serviceVersionId,
            UUID administratorId
    );

    WebsiteServiceVersion archiveVersion(
            UUID serviceVersionId,
            UUID administratorId
    );

    void deleteDraft(
            UUID serviceVersionId,
            UUID administratorId
    );

    WebsiteServiceVersion getPublicServiceByCode(
            String serviceCode
    );

    WebsiteServiceVersion getPublicServiceBySlug(
            String serviceSlug
    );

    List<WebsiteServiceVersion> getPublicServices();

    List<WebsiteServiceVersion> getFeaturedPublicServices();

    List<WebsiteServiceVersion> getBookablePublicServices();
}