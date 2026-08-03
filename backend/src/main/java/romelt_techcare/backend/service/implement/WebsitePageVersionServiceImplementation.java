package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.entity.WebsitePage;
import romelt_techcare.backend.entity.WebsitePageVersion;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;
import romelt_techcare.backend.enums.WebsiteMediaUsageResourceType;
import romelt_techcare.backend.enums.WebsitePageVersionStatus;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteMediaAssetRepository;
import romelt_techcare.backend.repository.WebsitePageRepository;
import romelt_techcare.backend.repository.WebsitePageVersionRepository;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;
import romelt_techcare.backend.service.WebsiteMediaUsageService;
import romelt_techcare.backend.service.WebsitePageVersionService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PAGE VERSION SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production draft, publication, archive, history, public
 * content, media-reference, and audit rules for website page
 * versions.
 *
 * Publication transaction:
 * 1. Locks the owning page.
 * 2. Locks the requested draft.
 * 3. Archives the currently published version, when present.
 * 4. Publishes the requested draft.
 * 5. Clears the page's draft pointer.
 * 6. Assigns the page's published pointer.
 * 7. Synchronizes the social-image media usage.
 * 8. Records all version lifecycle changes in the audit log.
 *
 * Audit behavior:
 * - Draft creation and updates use SAVE_DRAFT.
 * - Publication uses PUBLISH.
 * - Replaced published versions use ARCHIVE.
 * - Manual archival uses ARCHIVE.
 * - Permanent draft removal uses DELETE.
 *
 * Audit snapshot rule:
 * contentJson is stored because it is the actual administrator-managed
 * page content. Authentication data, storage credentials, and other
 * secrets must never be placed inside page content JSON.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsitePageVersionServiceImplementation
        implements WebsitePageVersionService {

    private static final String SOCIAL_IMAGE_USAGE_FIELD =
            "SOCIAL_IMAGE";

    private final WebsitePageVersionRepository
            websitePageVersionRepository;

    private final WebsitePageRepository
            websitePageRepository;

    private final WebsiteMediaAssetRepository
            websiteMediaAssetRepository;

    private final WebsiteMediaUsageService
            websiteMediaUsageService;

    private final AdminUserRepository
            adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Override
    @Transactional
    public WebsitePageVersion createDraft(
            UUID websitePageId,
            WebsitePageVersion requestedDraft,
            UUID administratorId
    ) {
        requireIdentifier(
                websitePageId,
                "Website page ID"
        );

        if (requestedDraft == null) {
            throw badRequest(
                    "Page draft information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePage page =
                getPageForUpdate(websitePageId);

        ensurePageUsable(page);
        ensureNoExistingDraft(websitePageId);

        validateDraftFields(
                requestedDraft,
                page
        );

        int nextVersionNumber =
                websitePageVersionRepository
                        .findMaximumVersionNumber(
                                websitePageId
                        )
                        + 1;

        WebsiteMediaAsset socialImage =
                validateSocialImage(
                        requestedDraft.getSocialImageMedia()
                );

        WebsitePageVersion draft =
                WebsitePageVersion.builder()
                        .websitePage(page)
                        .versionNumber(nextVersionNumber)
                        .versionStatus(
                                WebsitePageVersionStatus.DRAFT
                        )
                        .contentSchemaVersion(
                                requestedDraft
                                        .getContentSchemaVersion()
                        )
                        .contentJson(
                                requestedDraft.getContentJson()
                        )
                        .seoTitle(
                                normalizeOptional(
                                        requestedDraft.getSeoTitle()
                                )
                        )
                        .seoDescription(
                                normalizeOptional(
                                        requestedDraft
                                                .getSeoDescription()
                                )
                        )
                        .socialTitle(
                                normalizeOptional(
                                        requestedDraft
                                                .getSocialTitle()
                                )
                        )
                        .socialDescription(
                                normalizeOptional(
                                        requestedDraft
                                                .getSocialDescription()
                                )
                        )
                        .socialImageMedia(socialImage)
                        .canonicalUrl(
                                normalizeCanonicalUrl(
                                        requestedDraft
                                                .getCanonicalUrl()
                                )
                        )
                        .robotsIndex(
                                requestedDraft
                                        .getRobotsIndex() == null
                                        || requestedDraft
                                        .getRobotsIndex()
                        )
                        .robotsFollow(
                                requestedDraft
                                        .getRobotsFollow() == null
                                        || requestedDraft
                                        .getRobotsFollow()
                        )
                        .changeSummary(
                                normalizeOptional(
                                        requestedDraft
                                                .getChangeSummary()
                                )
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsitePageVersion savedDraft =
                saveVersion(
                        draft,
                        "Unable to create the page draft. The page may "
                                + "already have a current draft."
                );

        page.assignDraftVersion(
                savedDraft.getPageVersionId(),
                administrator
        );

        websitePageRepository.saveAndFlush(page);

        synchronizeSocialImageUsage(savedDraft);

        recordPageVersionAudit(
                administratorId,
                WebsiteContentAuditAction.SAVE_DRAFT,
                savedDraft,
                null,
                "Created website page draft version "
                        + savedDraft.getVersionNumber()
                        + "."
        );

        return getPageVersion(
                savedDraft.getPageVersionId()
        );
    }

    @Override
    @Transactional
    public WebsitePageVersion createDraftFromPublishedVersion(
            UUID websitePageId,
            String changeSummary,
            UUID administratorId
    ) {
        requireIdentifier(
                websitePageId,
                "Website page ID"
        );

        validateLength(
                changeSummary,
                1000,
                "Change summary"
        );

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePage page =
                getPageForUpdate(websitePageId);

        ensurePageUsable(page);
        ensureNoExistingDraft(websitePageId);

        WebsitePageVersion publishedVersion =
                websitePageVersionRepository
                        .findByPageAndStatusForUpdate(
                                websitePageId,
                                WebsitePageVersionStatus.PUBLISHED
                        )
                        .orElseThrow(() -> conflict(
                                "The page has no published version to copy."
                        ));

        int nextVersionNumber =
                websitePageVersionRepository
                        .findMaximumVersionNumber(
                                websitePageId
                        )
                        + 1;

        WebsitePageVersion draft =
                WebsitePageVersion.builder()
                        .websitePage(page)
                        .versionNumber(nextVersionNumber)
                        .versionStatus(
                                WebsitePageVersionStatus.DRAFT
                        )
                        .contentSchemaVersion(
                                publishedVersion
                                        .getContentSchemaVersion()
                        )
                        .contentJson(
                                publishedVersion
                                        .getContentJson()
                                        .deepCopy()
                        )
                        .seoTitle(
                                publishedVersion.getSeoTitle()
                        )
                        .seoDescription(
                                publishedVersion
                                        .getSeoDescription()
                        )
                        .socialTitle(
                                publishedVersion
                                        .getSocialTitle()
                        )
                        .socialDescription(
                                publishedVersion
                                        .getSocialDescription()
                        )
                        .socialImageMedia(
                                publishedVersion
                                        .getSocialImageMedia()
                        )
                        .canonicalUrl(
                                publishedVersion
                                        .getCanonicalUrl()
                        )
                        .robotsIndex(
                                publishedVersion
                                        .getRobotsIndex()
                        )
                        .robotsFollow(
                                publishedVersion
                                        .getRobotsFollow()
                        )
                        .changeSummary(
                                normalizeOptional(changeSummary)
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsitePageVersion savedDraft =
                saveVersion(
                        draft,
                        "Unable to create a draft from the published version."
                );

        page.assignDraftVersion(
                savedDraft.getPageVersionId(),
                administrator
        );

        websitePageRepository.saveAndFlush(page);

        synchronizeSocialImageUsage(savedDraft);

        recordPageVersionAudit(
                administratorId,
                WebsiteContentAuditAction.SAVE_DRAFT,
                savedDraft,
                createPageVersionSnapshot(publishedVersion),
                "Created website page draft version "
                        + savedDraft.getVersionNumber()
                        + " from published version "
                        + publishedVersion.getVersionNumber()
                        + "."
        );

        return getPageVersion(
                savedDraft.getPageVersionId()
        );
    }

    @Override
    @Transactional
    public WebsitePageVersion updateDraft(
            UUID pageVersionId,
            WebsitePageVersion requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                pageVersionId,
                "Page version ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated page draft information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePageVersion draft =
                getVersionForUpdate(pageVersionId);

        if (!draft.isDraft()) {
            throw conflict(
                    "Only a draft page version may be edited."
            );
        }

        WebsitePage page =
                draft.getWebsitePage();

        ensurePageUsable(page);

        if (
                page.getDraftVersionId() == null
                        || !pageVersionId.equals(
                        page.getDraftVersionId()
                )
        ) {
            throw conflict(
                    "The selected version is not the page's current draft."
            );
        }

        JsonNode beforeSnapshot =
                createPageVersionSnapshot(draft);

        validateDraftFields(
                requestedUpdate,
                page
        );

        WebsiteMediaAsset socialImage =
                validateSocialImage(
                        requestedUpdate
                                .getSocialImageMedia()
                );

        draft.updateDraft(
                requestedUpdate.getContentSchemaVersion(),
                requestedUpdate.getContentJson(),
                normalizeOptional(
                        requestedUpdate.getSeoTitle()
                ),
                normalizeOptional(
                        requestedUpdate.getSeoDescription()
                ),
                normalizeOptional(
                        requestedUpdate.getSocialTitle()
                ),
                normalizeOptional(
                        requestedUpdate
                                .getSocialDescription()
                ),
                socialImage,
                normalizeCanonicalUrl(
                        requestedUpdate.getCanonicalUrl()
                ),
                requestedUpdate.getRobotsIndex(),
                requestedUpdate.getRobotsFollow(),
                normalizeOptional(
                        requestedUpdate.getChangeSummary()
                ),
                administrator
        );

        WebsitePageVersion savedDraft =
                saveVersion(
                        draft,
                        "Unable to update the page draft."
                );

        synchronizeSocialImageUsage(savedDraft);

        recordPageVersionAudit(
                administratorId,
                WebsiteContentAuditAction.SAVE_DRAFT,
                savedDraft,
                beforeSnapshot,
                "Updated website page draft version "
                        + savedDraft.getVersionNumber()
                        + "."
        );

        return getPageVersion(pageVersionId);
    }

    @Override
    public WebsitePageVersion getPageVersion(
            UUID pageVersionId
    ) {
        requireIdentifier(
                pageVersionId,
                "Page version ID"
        );

        return websitePageVersionRepository
                .findByPageVersionId(pageVersionId)
                .orElseThrow(() -> notFound(
                        "Website page version was not found."
                ));
    }

    @Override
    public WebsitePageVersion getCurrentDraft(
            UUID websitePageId
    ) {
        requireIdentifier(
                websitePageId,
                "Website page ID"
        );

        return websitePageVersionRepository
                .findByWebsitePage_WebsitePageIdAndVersionStatus(
                        websitePageId,
                        WebsitePageVersionStatus.DRAFT
                )
                .orElseThrow(() -> notFound(
                        "The website page has no current draft."
                ));
    }

    @Override
    public WebsitePageVersion getCurrentPublishedVersion(
            UUID websitePageId
    ) {
        requireIdentifier(
                websitePageId,
                "Website page ID"
        );

        return websitePageVersionRepository
                .findByWebsitePage_WebsitePageIdAndVersionStatus(
                        websitePageId,
                        WebsitePageVersionStatus.PUBLISHED
                )
                .orElseThrow(() -> notFound(
                        "The website page has no published version."
                ));
    }

    @Override
    public Page<WebsitePageVersion> getVersionHistory(
            UUID websitePageId,
            WebsitePageVersionStatus versionStatus,
            Pageable pageable
    ) {
        requireIdentifier(
                websitePageId,
                "Website page ID"
        );

        requirePageable(pageable);

        if (!websitePageRepository.existsById(websitePageId)) {
            throw notFound(
                    "Website page was not found."
            );
        }

        if (versionStatus == null) {
            return websitePageVersionRepository
                    .findAllByWebsitePage_WebsitePageIdOrderByVersionNumberDesc(
                            websitePageId,
                            pageable
                    );
        }

        return websitePageVersionRepository
                .findAllByWebsitePage_WebsitePageIdAndVersionStatusOrderByVersionNumberDesc(
                        websitePageId,
                        versionStatus,
                        pageable
                );
    }

    @Override
    @Transactional
    public WebsitePageVersion publishDraft(
            UUID pageVersionId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePageVersion draft =
                getVersionForUpdate(pageVersionId);

        if (!draft.isDraft()) {
            throw conflict(
                    "Only a draft page version may be published."
            );
        }

        WebsitePage page =
                getPageForUpdate(
                        draft.getWebsitePage()
                                .getWebsitePageId()
                );

        ensurePageUsable(page);

        if (
                page.getDraftVersionId() == null
                        || !pageVersionId.equals(
                        page.getDraftVersionId()
                )
        ) {
            throw conflict(
                    "The selected version is not the page's current draft."
            );
        }

        validateDraftFields(draft, page);

        JsonNode draftBeforeSnapshot =
                createPageVersionSnapshot(draft);

        WebsitePageVersion currentPublished =
                websitePageVersionRepository
                        .findByPageAndStatusForUpdate(
                                page.getWebsitePageId(),
                                WebsitePageVersionStatus.PUBLISHED
                        )
                        .orElse(null);

        if (currentPublished != null) {
            JsonNode publishedBeforeSnapshot =
                    createPageVersionSnapshot(
                            currentPublished
                    );

            currentPublished.archive(administrator);

            WebsitePageVersion archivedVersion =
                    websitePageVersionRepository
                            .saveAndFlush(currentPublished);

            recordPageVersionAudit(
                    administratorId,
                    WebsiteContentAuditAction.ARCHIVE,
                    archivedVersion,
                    publishedBeforeSnapshot,
                    "Archived website page version "
                            + archivedVersion.getVersionNumber()
                            + " because version "
                            + draft.getVersionNumber()
                            + " was published."
            );
        }

        draft.publish(administrator);

        WebsitePageVersion publishedDraft =
                websitePageVersionRepository
                        .saveAndFlush(draft);

        page.assignPublishedVersion(
                publishedDraft.getPageVersionId(),
                administrator
        );

        page.clearDraftVersion(administrator);

        websitePageRepository.saveAndFlush(page);

        synchronizeSocialImageUsage(publishedDraft);

        recordPageVersionAudit(
                administratorId,
                WebsiteContentAuditAction.PUBLISH,
                publishedDraft,
                draftBeforeSnapshot,
                "Published website page version "
                        + publishedDraft.getVersionNumber()
                        + "."
        );

        return getPageVersion(
                publishedDraft.getPageVersionId()
        );
    }

    @Override
    @Transactional
    public WebsitePageVersion archiveVersion(
            UUID pageVersionId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePageVersion version =
                getVersionForUpdate(pageVersionId);

        WebsitePage page =
                getPageForUpdate(
                        version.getWebsitePage()
                                .getWebsitePageId()
                );

        if (version.isArchived()) {
            return getPageVersion(pageVersionId);
        }

        JsonNode beforeSnapshot =
                createPageVersionSnapshot(version);

        boolean wasDraft =
                version.isDraft();

        boolean wasPublished =
                version.isPublished();

        version.archive(administrator);

        WebsitePageVersion archivedVersion =
                websitePageVersionRepository
                        .saveAndFlush(version);

        if (
                wasDraft
                        && pageVersionId.equals(
                        page.getDraftVersionId()
                )
        ) {
            page.clearDraftVersion(administrator);
        }

        if (
                wasPublished
                        && pageVersionId.equals(
                        page.getPublishedVersionId()
                )
        ) {
            page.clearPublishedVersion(administrator);
        }

        websitePageRepository.saveAndFlush(page);

        recordPageVersionAudit(
                administratorId,
                WebsiteContentAuditAction.ARCHIVE,
                archivedVersion,
                beforeSnapshot,
                "Archived website page version "
                        + archivedVersion.getVersionNumber()
                        + "."
        );

        return getPageVersion(pageVersionId);
    }

    @Override
    @Transactional
    public void deleteDraft(
            UUID pageVersionId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePageVersion draft =
                getVersionForUpdate(pageVersionId);

        if (!draft.isDraft()) {
            throw conflict(
                    "Only a draft page version may be permanently deleted."
            );
        }

        JsonNode beforeSnapshot =
                createPageVersionSnapshot(draft);

        UUID deletedVersionId =
                draft.getPageVersionId();

        int deletedVersionNumber =
                draft.getVersionNumber();

        String resourceName =
                createPageVersionResourceName(draft);

        WebsitePage page =
                getPageForUpdate(
                        draft.getWebsitePage()
                                .getWebsitePageId()
                );

        websiteMediaUsageService
                .removeAllUsagesForResource(
                        WebsiteMediaUsageResourceType
                                .WEBSITE_PAGE_VERSION,
                        pageVersionId
                );

        if (
                pageVersionId.equals(
                        page.getDraftVersionId()
                )
        ) {
            page.clearDraftVersion(administrator);
            websitePageRepository.saveAndFlush(page);
        }

        websitePageVersionRepository.delete(draft);
        websitePageVersionRepository.flush();

        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.DELETE,
                WebsiteContentAuditResourceType
                        .WEBSITE_PAGE_VERSION,
                deletedVersionId,
                resourceName,
                beforeSnapshot,
                null,
                "Permanently deleted website page draft version "
                        + deletedVersionNumber
                        + ".",
                null
        );
    }

    @Override
    public WebsitePageVersion getPublicPageByKey(
            String pageKey
    ) {
        return websitePageVersionRepository
                .findPublicByPageKey(
                        normalizePageKey(pageKey)
                )
                .orElseThrow(() -> notFound(
                        "Public website page was not found."
                ));
    }

    @Override
    public WebsitePageVersion getPublicPageByRoute(
            String routePath
    ) {
        return websitePageVersionRepository
                .findPublicByRoutePath(
                        normalizeRoutePath(routePath)
                )
                .orElseThrow(() -> notFound(
                        "Public website page was not found."
                ));
    }

    private void recordPageVersionAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsitePageVersion version,
            JsonNode beforeSnapshot,
            String changeSummary
    ) {
        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType
                        .WEBSITE_PAGE_VERSION,
                version.getPageVersionId(),
                createPageVersionResourceName(version),
                beforeSnapshot,
                createPageVersionSnapshot(version),
                changeSummary,
                null
        );
    }

    private JsonNode createPageVersionSnapshot(
            WebsitePageVersion version
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "pageVersionId",
                version.getPageVersionId()
        );

        fields.put(
                "websitePageId",
                version.getWebsitePage() == null
                        ? null
                        : version.getWebsitePage()
                        .getWebsitePageId()
        );

        fields.put(
                "pageKey",
                version.getWebsitePage() == null
                        ? null
                        : version.getWebsitePage()
                        .getPageKey()
        );

        fields.put(
                "versionNumber",
                version.getVersionNumber()
        );

        fields.put(
                "versionStatus",
                version.getVersionStatus()
        );

        fields.put(
                "contentSchemaVersion",
                version.getContentSchemaVersion()
        );

        fields.put(
                "contentJson",
                version.getContentJson() == null
                        ? null
                        : version.getContentJson().deepCopy()
        );

        fields.put(
                "seoTitle",
                version.getSeoTitle()
        );

        fields.put(
                "seoDescription",
                version.getSeoDescription()
        );

        fields.put(
                "socialTitle",
                version.getSocialTitle()
        );

        fields.put(
                "socialDescription",
                version.getSocialDescription()
        );

        fields.put(
                "socialImageMediaId",
                version.getSocialImageMedia() == null
                        ? null
                        : version.getSocialImageMedia()
                        .getMediaAssetId()
        );

        fields.put(
                "canonicalUrl",
                version.getCanonicalUrl()
        );

        fields.put(
                "robotsIndex",
                version.getRobotsIndex()
        );

        fields.put(
                "robotsFollow",
                version.getRobotsFollow()
        );

        fields.put(
                "changeSummary",
                version.getChangeSummary()
        );

        fields.put(
                "publishedAt",
                version.getPublishedAt()
        );

        fields.put(
                "archivedAt",
                version.getArchivedAt()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    private String createPageVersionResourceName(
            WebsitePageVersion version
    ) {
        String pageKey =
                version.getWebsitePage() == null
                        ? null
                        : normalizeOptional(
                        version.getWebsitePage()
                                .getPageKey()
                );

        if (pageKey == null) {
            pageKey = "WEBSITE_PAGE";
        }

        return pageKey
                + " - Version "
                + version.getVersionNumber();
    }

    private WebsitePageVersion getVersionForUpdate(
            UUID pageVersionId
    ) {
        requireIdentifier(
                pageVersionId,
                "Page version ID"
        );

        return websitePageVersionRepository
                .findByIdForUpdate(pageVersionId)
                .orElseThrow(() -> notFound(
                        "Website page version was not found."
                ));
    }

    private WebsitePage getPageForUpdate(
            UUID websitePageId
    ) {
        requireIdentifier(
                websitePageId,
                "Website page ID"
        );

        return websitePageRepository
                .findByIdForUpdate(websitePageId)
                .orElseThrow(() -> notFound(
                        "Website page was not found."
                ));
    }

    private void ensureNoExistingDraft(
            UUID websitePageId
    ) {
        if (
                websitePageVersionRepository
                        .existsByWebsitePage_WebsitePageIdAndVersionStatus(
                                websitePageId,
                                WebsitePageVersionStatus.DRAFT
                        )
        ) {
            throw conflict(
                    "The website page already has a current draft."
            );
        }
    }

    private void ensurePageUsable(
            WebsitePage page
    ) {
        if (page == null || page.isDeleted()) {
            throw conflict(
                    "A deleted website page cannot manage content versions."
            );
        }
    }

    private void validateDraftFields(
            WebsitePageVersion draft,
            WebsitePage page
    ) {
        if (
                draft.getContentSchemaVersion() == null
                        || draft.getContentSchemaVersion() <= 0
        ) {
            throw badRequest(
                    "Content schema version must be greater than zero."
            );
        }

        if (
                !draft.getContentSchemaVersion().equals(
                        page.getContentSchemaVersion()
                )
        ) {
            throw conflict(
                    "The draft content schema version must match the "
                            + "website page content schema version."
            );
        }

        JsonNode contentJson =
                draft.getContentJson();

        if (contentJson == null || !contentJson.isObject()) {
            throw badRequest(
                    "Page content must be a JSON object."
            );
        }

        validateLength(
                draft.getSeoTitle(),
                255,
                "SEO title"
        );

        validateLength(
                draft.getSeoDescription(),
                500,
                "SEO description"
        );

        validateLength(
                draft.getSocialTitle(),
                255,
                "Social title"
        );

        validateLength(
                draft.getSocialDescription(),
                500,
                "Social description"
        );

        validateLength(
                draft.getCanonicalUrl(),
                1000,
                "Canonical URL"
        );

        validateLength(
                draft.getChangeSummary(),
                1000,
                "Change summary"
        );

        normalizeCanonicalUrl(
                draft.getCanonicalUrl()
        );
    }

    private WebsiteMediaAsset validateSocialImage(
            WebsiteMediaAsset requestedMedia
    ) {
        if (
                requestedMedia == null
                        || requestedMedia
                        .getMediaAssetId() == null
        ) {
            return null;
        }

        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetRepository
                        .findByMediaAssetIdAndDeletedAtIsNull(
                                requestedMedia
                                        .getMediaAssetId()
                        )
                        .orElseThrow(() -> notFound(
                                "Social-image media asset was not found."
                        ));

        if (
                mediaAsset.getAssetStatus()
                        != WebsiteMediaAssetStatus.ACTIVE
        ) {
            throw conflict(
                    "The social image must reference an active media asset."
            );
        }

        if (!Boolean.TRUE.equals(mediaAsset.getIsPublic())) {
            throw conflict(
                    "The social image must reference a public media asset."
            );
        }

        if (!mediaAsset.isPubliclyAvailable()) {
            throw conflict(
                    "The social image is not publicly available."
            );
        }

        return mediaAsset;
    }

    private void synchronizeSocialImageUsage(
            WebsitePageVersion version
    ) {
        UUID mediaAssetId =
                version.getSocialImageMedia() == null
                        ? null
                        : version.getSocialImageMedia()
                        .getMediaAssetId();

        websiteMediaUsageService.replaceUsage(
                mediaAssetId,
                WebsiteMediaUsageResourceType
                        .WEBSITE_PAGE_VERSION,
                version.getPageVersionId(),
                SOCIAL_IMAGE_USAGE_FIELD,
                "Social-sharing image for website page version "
                        + version.getVersionNumber()
                        + "."
        );
    }

    private String normalizeCanonicalUrl(
            String value
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            return null;
        }

        try {
            URI uri =
                    new URI(normalized);

            String scheme =
                    uri.getScheme();

            String host =
                    uri.getHost();

            if (
                    scheme == null
                            || host == null
                            || (
                            !"http".equalsIgnoreCase(scheme)
                                    && !"https".equalsIgnoreCase(
                                    scheme
                            )
                    )
            ) {
                throw badRequest(
                        "Canonical URL must be a valid HTTP or HTTPS URL."
                );
            }

            return uri.normalize().toString();
        } catch (URISyntaxException exception) {
            throw badRequest(
                    "Canonical URL must be valid."
            );
        }
    }

    private String normalizePageKey(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Page key"
                )
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    "Page key is required."
            );
        }

        return normalized;
    }

    private String normalizeRoutePath(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Route path"
                );

        if (!normalized.startsWith("/")) {
            normalized =
                    "/" + normalized;
        }

        normalized =
                normalized.replaceAll(
                        "/{2,}",
                        "/"
                );

        if (
                normalized.length() > 1
                        && normalized.endsWith("/")
        ) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    );
        }

        return normalized;
    }

    private AdminUser getRequiredAdministrator(
            UUID administratorId
    ) {
        requireIdentifier(
                administratorId,
                "Administrator ID"
        );

        return adminUserRepository
                .findById(administratorId)
                .orElseThrow(() -> notFound(
                        "Administrator account was not found."
                ));
    }

    private WebsitePageVersion saveVersion(
            WebsitePageVersion version,
            String conflictMessage
    ) {
        try {
            return websitePageVersionRepository
                    .saveAndFlush(version);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    conflictMessage,
                    exception
            );
        }
    }

    private void validateLength(
            String value,
            int maximumLength,
            String fieldName
    ) {
        if (
                value != null
                        && value.trim().length()
                        > maximumLength
        ) {
            throw badRequest(
                    fieldName
                            + " must not exceed "
                            + maximumLength
                            + " characters."
            );
        }
    }

    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw badRequest(
                    "Pagination information is required."
            );
        }
    }

    private void requireIdentifier(
            UUID identifier,
            String fieldName
    ) {
        if (identifier == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }
    }

    private String normalizeRequired(
            String value,
            String fieldName
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }

        return normalized;
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}