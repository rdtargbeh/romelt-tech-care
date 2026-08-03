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
import romelt_techcare.backend.entity.WebsitePage;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.enums.WebsitePageType;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsitePageRepository;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;
import romelt_techcare.backend.service.WebsitePageService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production business rules for stable website-page
 * identities.
 *
 * Responsibilities:
 * - Creates and updates stable website pages.
 * - Normalizes page keys and route paths.
 * - Enforces unique page keys and routes.
 * - Controls active and inactive page status.
 * - Controls draft and published version pointers.
 * - Supports soft deletion and restoration.
 * - Protects system pages from deletion.
 * - Retrieves administrator and public website pages.
 * - Attributes write operations to administrators.
 * - Records immutable content audit entries for every mutation.
 *
 * Version lifecycle:
 * Page-version content creation, editing, publication, archival, and
 * permanent draft deletion are managed by
 * WebsitePageVersionServiceImplementation.
 *
 * Audit behavior:
 * - Page creation uses CREATE.
 * - Page metadata changes use UPDATE.
 * - Draft-pointer assignment uses SAVE_DRAFT.
 * - Published-pointer assignment uses PUBLISH.
 * - Published-pointer removal uses UNPUBLISH.
 * - Activation and deactivation use UPDATE.
 * - Soft deletion uses DELETE.
 * - Restoration uses RESTORE.
 *
 * Transaction behavior:
 * Each page mutation and its corresponding audit entry occur in the
 * same transaction.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsitePageServiceImplementation
        implements WebsitePageService {

    private final WebsitePageRepository
            websitePageRepository;

    private final AdminUserRepository
            adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    @Override
    @Transactional
    public WebsitePage createWebsitePage(
            WebsitePage websitePage,
            UUID administratorId
    ) {
        if (websitePage == null) {
            throw badRequest(
                    "Website page information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        String pageKey =
                normalizePageKey(
                        websitePage.getPageKey()
                );

        String routePath =
                normalizeRoutePath(
                        websitePage.getRoutePath()
                );

        validatePageFields(websitePage);

        if (
                websitePageRepository
                        .existsByPageKey(pageKey)
        ) {
            throw conflict(
                    "A website page already uses this page key."
            );
        }

        if (
                websitePageRepository
                        .existsByRoutePath(routePath)
        ) {
            throw conflict(
                    "A website page already uses this route path."
            );
        }

        WebsitePage pageToCreate =
                WebsitePage.builder()
                        .pageKey(pageKey)
                        .pageName(
                                normalizeRequired(
                                        websitePage.getPageName(),
                                        "Page name"
                                )
                        )
                        .routePath(routePath)
                        .pageType(
                                websitePage.getPageType() == null
                                        ? WebsitePageType.STANDARD
                                        : websitePage.getPageType()
                        )
                        .draftVersionId(
                                websitePage.getDraftVersionId()
                        )
                        .publishedVersionId(
                                websitePage.getPublishedVersionId()
                        )
                        .contentSchemaVersion(
                                websitePage
                                        .getContentSchemaVersion() == null
                                        ? 1
                                        : websitePage
                                        .getContentSchemaVersion()
                        )
                        .isSystemPage(
                                websitePage.getIsSystemPage() == null
                                        || websitePage.getIsSystemPage()
                        )
                        .isActive(
                                websitePage.getIsActive() == null
                                        || websitePage.getIsActive()
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsitePage savedPage =
                saveWebsitePage(
                        pageToCreate,
                        "Unable to create the website page because its "
                                + "key or route is already in use."
                );

        recordPageAudit(
                administratorId,
                WebsiteContentAuditAction.CREATE,
                savedPage,
                null,
                "Website page created."
        );

        return savedPage;
    }

    @Override
    @Transactional
    public WebsitePage updateWebsitePage(
            UUID websitePageId,
            WebsitePage requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                websitePageId,
                "Website page ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated website page information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePage existingPage =
                getWebsitePageForUpdate(
                        websitePageId
                );

        JsonNode beforeSnapshot =
                createPageSnapshot(existingPage);

        String pageKey =
                normalizePageKey(
                        requestedUpdate.getPageKey()
                );

        String routePath =
                normalizeRoutePath(
                        requestedUpdate.getRoutePath()
                );

        validatePageFields(requestedUpdate);

        if (
                websitePageRepository
                        .existsByPageKeyAndWebsitePageIdNot(
                                pageKey,
                                websitePageId
                        )
        ) {
            throw conflict(
                    "Another website page already uses this page key."
            );
        }

        if (
                websitePageRepository
                        .existsByRoutePathAndWebsitePageIdNot(
                                routePath,
                                websitePageId
                        )
        ) {
            throw conflict(
                    "Another website page already uses this route path."
            );
        }

        existingPage.updateDetails(
                pageKey,
                normalizeRequired(
                        requestedUpdate.getPageName(),
                        "Page name"
                ),
                routePath,
                requestedUpdate.getPageType(),
                requestedUpdate.getContentSchemaVersion(),
                requestedUpdate.getIsSystemPage(),
                requestedUpdate.getIsActive(),
                administrator
        );

        WebsitePage savedPage =
                saveWebsitePage(
                        existingPage,
                        "Unable to update the website page because its "
                                + "key or route is already in use."
                );

        recordPageAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedPage,
                beforeSnapshot,
                "Website page details updated."
        );

        return savedPage;
    }

    @Override
    public WebsitePage getWebsitePage(
            UUID websitePageId
    ) {
        requireIdentifier(
                websitePageId,
                "Website page ID"
        );

        return websitePageRepository
                .findByWebsitePageIdAndDeletedAtIsNull(
                        websitePageId
                )
                .orElseThrow(() -> notFound(
                        "Website page was not found."
                ));
    }

    @Override
    public WebsitePage getWebsitePageByKey(
            String pageKey
    ) {
        return websitePageRepository
                .findByPageKeyAndDeletedAtIsNull(
                        normalizePageKey(pageKey)
                )
                .orElseThrow(() -> notFound(
                        "Website page was not found."
                ));
    }

    @Override
    public WebsitePage getWebsitePageByRoute(
            String routePath
    ) {
        return websitePageRepository
                .findByRoutePathAndDeletedAtIsNull(
                        normalizeRoutePath(routePath)
                )
                .orElseThrow(() -> notFound(
                        "Website page was not found."
                ));
    }

    @Override
    public Page<WebsitePage> searchWebsitePages(
            String keyword,
            WebsitePageType pageType,
            Boolean isSystemPage,
            Boolean isActive,
            Pageable pageable
    ) {
        requirePageable(pageable);

        return websitePageRepository.searchWebsitePages(
                normalizeOptional(keyword),
                pageType,
                isSystemPage,
                isActive,
                pageable
        );
    }

    @Override
    public Page<WebsitePage> getDeletedWebsitePages(
            Pageable pageable
    ) {
        requirePageable(pageable);

        return websitePageRepository
                .findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(
                        pageable
                );
    }

    @Override
    @Transactional
    public WebsitePage assignDraftVersion(
            UUID websitePageId,
            UUID pageVersionId,
            UUID administratorId
    ) {
        requireIdentifier(
                pageVersionId,
                "Page version ID"
        );

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePage page =
                getWebsitePageForUpdate(
                        websitePageId
                );

        ensurePageNotDeleted(page);

        JsonNode beforeSnapshot =
                createPageSnapshot(page);

        page.assignDraftVersion(
                pageVersionId,
                administrator
        );

        WebsitePage savedPage =
                websitePageRepository.saveAndFlush(page);

        recordPageAudit(
                administratorId,
                WebsiteContentAuditAction.SAVE_DRAFT,
                savedPage,
                beforeSnapshot,
                "Assigned draft version "
                        + pageVersionId
                        + " to website page."
        );

        return savedPage;
    }

    @Override
    @Transactional
    public WebsitePage clearDraftVersion(
            UUID websitePageId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePage page =
                getWebsitePageForUpdate(
                        websitePageId
                );

        ensurePageNotDeleted(page);

        if (page.getDraftVersionId() == null) {
            return page;
        }

        UUID previousDraftVersionId =
                page.getDraftVersionId();

        JsonNode beforeSnapshot =
                createPageSnapshot(page);

        page.clearDraftVersion(administrator);

        WebsitePage savedPage =
                websitePageRepository.saveAndFlush(page);

        recordPageAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedPage,
                beforeSnapshot,
                "Cleared draft version "
                        + previousDraftVersionId
                        + " from website page."
        );

        return savedPage;
    }

    @Override
    @Transactional
    public WebsitePage assignPublishedVersion(
            UUID websitePageId,
            UUID pageVersionId,
            UUID administratorId
    ) {
        requireIdentifier(
                pageVersionId,
                "Page version ID"
        );

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePage page =
                getWebsitePageForUpdate(
                        websitePageId
                );

        ensurePageNotDeleted(page);

        JsonNode beforeSnapshot =
                createPageSnapshot(page);

        page.assignPublishedVersion(
                pageVersionId,
                administrator
        );

        WebsitePage savedPage =
                websitePageRepository.saveAndFlush(page);

        recordPageAudit(
                administratorId,
                WebsiteContentAuditAction.PUBLISH,
                savedPage,
                beforeSnapshot,
                "Assigned published version "
                        + pageVersionId
                        + " to website page."
        );

        return savedPage;
    }

    @Override
    @Transactional
    public WebsitePage clearPublishedVersion(
            UUID websitePageId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePage page =
                getWebsitePageForUpdate(
                        websitePageId
                );

        ensurePageNotDeleted(page);

        if (page.getPublishedVersionId() == null) {
            return page;
        }

        UUID previousPublishedVersionId =
                page.getPublishedVersionId();

        JsonNode beforeSnapshot =
                createPageSnapshot(page);

        page.clearPublishedVersion(administrator);

        WebsitePage savedPage =
                websitePageRepository.saveAndFlush(page);

        recordPageAudit(
                administratorId,
                WebsiteContentAuditAction.UNPUBLISH,
                savedPage,
                beforeSnapshot,
                "Cleared published version "
                        + previousPublishedVersionId
                        + " from website page."
        );

        return savedPage;
    }

    @Override
    @Transactional
    public WebsitePage updateWebsitePageStatus(
            UUID websitePageId,
            boolean isActive,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePage page =
                getWebsitePageForUpdate(
                        websitePageId
                );

        ensurePageNotDeleted(page);

        if (
                Boolean.TRUE.equals(page.getIsActive())
                        == isActive
        ) {
            return page;
        }

        JsonNode beforeSnapshot =
                createPageSnapshot(page);

        if (isActive) {
            page.activate(administrator);
        } else {
            page.deactivate(administrator);
        }

        WebsitePage savedPage =
                websitePageRepository.saveAndFlush(page);

        recordPageAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedPage,
                beforeSnapshot,
                isActive
                        ? "Website page activated."
                        : "Website page deactivated."
        );

        return savedPage;
    }

    @Override
    @Transactional
    public WebsitePage activateWebsitePage(
            UUID websitePageId,
            UUID administratorId
    ) {
        return updateWebsitePageStatus(
                websitePageId,
                true,
                administratorId
        );
    }

    @Override
    @Transactional
    public WebsitePage deactivateWebsitePage(
            UUID websitePageId,
            UUID administratorId
    ) {
        return updateWebsitePageStatus(
                websitePageId,
                false,
                administratorId
        );
    }

    @Override
    @Transactional
    public void deleteWebsitePage(
            UUID websitePageId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePage page =
                getWebsitePageForUpdate(
                        websitePageId
                );

        if (Boolean.TRUE.equals(page.getIsSystemPage())) {
            throw conflict(
                    "A system website page cannot be deleted. "
                            + "Deactivate it instead."
            );
        }

        if (page.isDeleted()) {
            throw conflict(
                    "The website page is already deleted."
            );
        }

        JsonNode beforeSnapshot =
                createPageSnapshot(page);

        page.softDelete(administrator);

        WebsitePage deletedPage =
                websitePageRepository.saveAndFlush(page);

        recordPageAudit(
                administratorId,
                WebsiteContentAuditAction.DELETE,
                deletedPage,
                beforeSnapshot,
                "Website page soft deleted."
        );
    }

    @Override
    @Transactional
    public WebsitePage restoreWebsitePage(
            UUID websitePageId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePage page =
                getDeletedWebsitePageForUpdate(
                        websitePageId
                );

        if (!page.isDeleted()) {
            throw conflict(
                    "The website page is not deleted."
            );
        }

        JsonNode beforeSnapshot =
                createPageSnapshot(page);

        page.restore(administrator);

        /*
         * Restoration does not automatically activate or publish the
         * page. An administrator must explicitly review and activate it.
         */
        page.setIsActive(false);

        WebsitePage restoredPage =
                websitePageRepository.saveAndFlush(page);

        recordPageAudit(
                administratorId,
                WebsiteContentAuditAction.RESTORE,
                restoredPage,
                beforeSnapshot,
                "Website page restored. The restored page remains inactive."
        );

        return restoredPage;
    }

    @Override
    public WebsitePage getPublicWebsitePageByKey(
            String pageKey
    ) {
        return websitePageRepository
                .findByPageKeyAndIsActiveTrueAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
                        normalizePageKey(pageKey)
                )
                .orElseThrow(() -> notFound(
                        "Public website page was not found."
                ));
    }

    @Override
    public WebsitePage getPublicWebsitePageByRoute(
            String routePath
    ) {
        return websitePageRepository
                .findByRoutePathAndIsActiveTrueAndDeletedAtIsNullAndPublishedVersionIdIsNotNull(
                        normalizeRoutePath(routePath)
                )
                .orElseThrow(() -> notFound(
                        "Public website page was not found."
                ));
    }

    @Override
    public List<WebsitePage> getPublicWebsitePages() {
        return websitePageRepository
                .findAllByIsActiveTrueAndDeletedAtIsNullAndPublishedVersionIdIsNotNullOrderByPageNameAsc();
    }

    @Override
    public long countActiveWebsitePages() {
        return websitePageRepository
                .countByIsActiveTrueAndDeletedAtIsNull();
    }

    @Override
    public long countDeletedWebsitePages() {
        return websitePageRepository
                .countByDeletedAtIsNotNull();
    }

    private void recordPageAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            WebsitePage page,
            JsonNode beforeSnapshot,
            String changeSummary
    ) {
        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType.WEBSITE_PAGE,
                page.getWebsitePageId(),
                page.getPageName(),
                beforeSnapshot,
                createPageSnapshot(page),
                changeSummary,
                null
        );
    }

    private JsonNode createPageSnapshot(
            WebsitePage page
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "websitePageId",
                page.getWebsitePageId()
        );

        fields.put(
                "pageKey",
                page.getPageKey()
        );

        fields.put(
                "pageName",
                page.getPageName()
        );

        fields.put(
                "routePath",
                page.getRoutePath()
        );

        fields.put(
                "pageType",
                page.getPageType()
        );

        fields.put(
                "draftVersionId",
                page.getDraftVersionId()
        );

        fields.put(
                "publishedVersionId",
                page.getPublishedVersionId()
        );

        fields.put(
                "contentSchemaVersion",
                page.getContentSchemaVersion()
        );

        fields.put(
                "isSystemPage",
                page.getIsSystemPage()
        );

        fields.put(
                "isActive",
                page.getIsActive()
        );

        fields.put(
                "deleted",
                page.isDeleted()
        );

        fields.put(
                "deletedAt",
                page.getDeletedAt()
        );

        fields.put(
                "createdAt",
                page.getCreatedAt()
        );

        fields.put(
                "updatedAt",
                page.getUpdatedAt()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(fields);
    }

    private WebsitePage getWebsitePageForUpdate(
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

    private WebsitePage getDeletedWebsitePageForUpdate(
            UUID websitePageId
    ) {
        requireIdentifier(
                websitePageId,
                "Website page ID"
        );

        return websitePageRepository
                .findIncludingDeletedByIdForUpdate(
                        websitePageId
                )
                .orElseThrow(() -> notFound(
                        "Website page was not found."
                ));
    }

    private void ensurePageNotDeleted(
            WebsitePage page
    ) {
        if (page == null || page.isDeleted()) {
            throw conflict(
                    "A deleted website page cannot be modified."
            );
        }
    }

    private void validatePageFields(
            WebsitePage page
    ) {
        if (isBlank(page.getPageKey())) {
            throw badRequest(
                    "Page key is required."
            );
        }

        if (
                page.getPageKey()
                        .trim()
                        .length() > 120
        ) {
            throw badRequest(
                    "Page key must not exceed 120 characters."
            );
        }

        if (isBlank(page.getPageName())) {
            throw badRequest(
                    "Page name is required."
            );
        }

        if (
                page.getPageName()
                        .trim()
                        .length() > 180
        ) {
            throw badRequest(
                    "Page name must not exceed 180 characters."
            );
        }

        if (isBlank(page.getRoutePath())) {
            throw badRequest(
                    "Route path is required."
            );
        }

        if (
                page.getRoutePath()
                        .trim()
                        .length() > 500
        ) {
            throw badRequest(
                    "Route path must not exceed 500 characters."
            );
        }

        if (page.getPageType() == null) {
            throw badRequest(
                    "Page type is required."
            );
        }

        if (
                page.getContentSchemaVersion() == null
                        || page.getContentSchemaVersion() <= 0
        ) {
            throw badRequest(
                    "Content schema version must be greater than zero."
            );
        }

        if (page.getIsSystemPage() == null) {
            throw badRequest(
                    "System-page status is required."
            );
        }

        if (page.getIsActive() == null) {
            throw badRequest(
                    "Active status is required."
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
                        .replaceAll(
                                "[^A-Z0-9]+",
                                "_"
                        )
                        .replaceAll(
                                "^_+|_+$",
                                ""
                        );

        if (normalized.isBlank()) {
            throw badRequest(
                    "Page key is required."
            );
        }

        if (normalized.length() > 120) {
            throw badRequest(
                    "Normalized page key must not exceed 120 characters."
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

        if (normalized.length() > 500) {
            throw badRequest(
                    "Route path must not exceed 500 characters."
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

    private WebsitePage saveWebsitePage(
            WebsitePage page,
            String conflictMessage
    ) {
        try {
            return websitePageRepository
                    .saveAndFlush(page);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    conflictMessage,
                    exception
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

    private boolean isBlank(
            String value
    ) {
        return value == null
                || value.trim().isEmpty();
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