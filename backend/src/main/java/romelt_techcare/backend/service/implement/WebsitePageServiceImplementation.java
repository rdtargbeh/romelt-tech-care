package romelt_techcare.backend.service.implement;

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
import romelt_techcare.backend.enums.WebsitePageType;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsitePageRepository;
import romelt_techcare.backend.service.WebsitePageService;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production business rules for stable website pages.
 *
 * Version validation:
 * The supplied schema contains draft_version_id and
 * published_version_id but does not yet provide the page-version table
 * or foreign keys. This implementation stores the UUID pointers.
 * Ownership and lifecycle validation will be connected when the
 * page-version entity is provided.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsitePageServiceImplementation
        implements WebsitePageService {

    private final WebsitePageRepository websitePageRepository;
    private final AdminUserRepository adminUserRepository;

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
                normalizePageKey(websitePage.getPageKey());

        String routePath =
                normalizeRoutePath(websitePage.getRoutePath());

        validatePageFields(websitePage);

        if (websitePageRepository.existsByPageKey(pageKey)) {
            throw conflict(
                    "A website page already uses this page key."
            );
        }

        if (websitePageRepository.existsByRoutePath(routePath)) {
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

        return saveWebsitePage(
                pageToCreate,
                "Unable to create the website page because its key or "
                        + "route is already in use."
        );
    }

    @Override
    @Transactional
    public WebsitePage updateWebsitePage(
            UUID websitePageId,
            WebsitePage requestedUpdate,
            UUID administratorId
    ) {
        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated website page information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsitePage existingPage =
                getWebsitePageForUpdate(websitePageId);

        String pageKey =
                normalizePageKey(requestedUpdate.getPageKey());

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

        return saveWebsitePage(
                existingPage,
                "Unable to update the website page because its key or "
                        + "route is already in use."
        );
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
                getWebsitePageForUpdate(websitePageId);

        page.assignDraftVersion(
                pageVersionId,
                administrator
        );

        return websitePageRepository.save(page);
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
                getWebsitePageForUpdate(websitePageId);

        page.clearDraftVersion(administrator);

        return websitePageRepository.save(page);
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
                getWebsitePageForUpdate(websitePageId);

        page.assignPublishedVersion(
                pageVersionId,
                administrator
        );

        return websitePageRepository.save(page);
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
                getWebsitePageForUpdate(websitePageId);

        page.clearPublishedVersion(administrator);

        return websitePageRepository.save(page);
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
                getWebsitePageForUpdate(websitePageId);

        if (isActive) {
            page.activate(administrator);
        } else {
            page.deactivate(administrator);
        }

        return websitePageRepository.save(page);
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
                getWebsitePageForUpdate(websitePageId);

        if (Boolean.TRUE.equals(page.getIsSystemPage())) {
            throw conflict(
                    "A system website page cannot be deleted. "
                            + "Deactivate it instead."
            );
        }

        page.softDelete(administrator);

        websitePageRepository.save(page);
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

        page.restore(administrator);

        /*
         * Restoration does not automatically publish or activate a page.
         */
        page.setIsActive(false);

        return websitePageRepository.save(page);
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

    private void validatePageFields(
            WebsitePage page
    ) {
        if (isBlank(page.getPageKey())) {
            throw badRequest("Page key is required.");
        }

        if (page.getPageKey().trim().length() > 120) {
            throw badRequest(
                    "Page key must not exceed 120 characters."
            );
        }

        if (isBlank(page.getPageName())) {
            throw badRequest("Page name is required.");
        }

        if (page.getPageName().trim().length() > 180) {
            throw badRequest(
                    "Page name must not exceed 180 characters."
            );
        }

        if (isBlank(page.getRoutePath())) {
            throw badRequest("Route path is required.");
        }

        if (page.getRoutePath().trim().length() > 500) {
            throw badRequest(
                    "Route path must not exceed 500 characters."
            );
        }

        if (page.getPageType() == null) {
            throw badRequest("Page type is required.");
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
                normalizeRequired(value, "Page key")
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw badRequest("Page key is required.");
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
                normalizeRequired(value, "Route path");

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        normalized = normalized.replaceAll("/{2,}", "/");

        if (
                normalized.length() > 1
                        && normalized.endsWith("/")
        ) {
            normalized = normalized.substring(
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

        return adminUserRepository.findById(administratorId)
                .orElseThrow(() -> notFound(
                        "Administrator account was not found."
                ));
    }

    private WebsitePage saveWebsitePage(
            WebsitePage page,
            String conflictMessage
    ) {
        try {
            return websitePageRepository.saveAndFlush(page);
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
        String normalized = normalizeOptional(value);

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

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private boolean isBlank(
            String value
    ) {
        return value == null || value.trim().isEmpty();
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