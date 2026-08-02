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
import romelt_techcare.backend.entity.WebsiteNavigationItem;
import romelt_techcare.backend.entity.WebsitePage;
import romelt_techcare.backend.enums.WebsiteNavigationDestinationType;
import romelt_techcare.backend.enums.WebsiteNavigationLocation;
import romelt_techcare.backend.enums.WebsiteNavigationTargetBehavior;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteNavigationItemRepository;
import romelt_techcare.backend.repository.WebsitePageRepository;
import romelt_techcare.backend.service.WebsiteNavigationItemService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — NAVIGATION ITEM SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production rules for website navigation management.
 *
 * Destination rules:
 * - INTERNAL_ROUTE may reference an active WebsitePage or an explicit
 *   internal route beginning with "/".
 * - EXTERNAL_URL requires a valid HTTP or HTTPS URL.
 * - ANCHOR requires a value containing or beginning with "#".
 * - ACTION requires a stable frontend-recognized action key.
 *
 * Page relationship:
 * When a WebsitePage is supplied for INTERNAL_ROUTE, its routePath is
 * authoritative. The stored destinationUrl may remain null.
 *
 * Public behavior:
 * Public results include only visible, non-deleted items. Items linked
 * to inactive or deleted WebsitePage records are filtered by the
 * service before being returned.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteNavigationItemServiceImplementation
        implements WebsiteNavigationItemService {

    private final WebsiteNavigationItemRepository
            websiteNavigationItemRepository;

    private final WebsitePageRepository websitePageRepository;

    private final AdminUserRepository adminUserRepository;

    @Override
    @Transactional
    public WebsiteNavigationItem createNavigationItem(
            WebsiteNavigationItem navigationItem,
            UUID administratorId
    ) {
        if (navigationItem == null) {
            throw badRequest(
                    "Website navigation-item information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        validateEditableFields(navigationItem);

        WebsiteNavigationLocation location =
                navigationItem.getNavigationLocation();

        String itemKey =
                normalizeItemKey(
                        navigationItem.getItemKey()
                );

        if (
                websiteNavigationItemRepository
                        .existsByNavigationLocationAndItemKey(
                                location,
                                itemKey
                        )
        ) {
            throw conflict(
                    "A navigation item already exists with this "
                            + "location and item key."
            );
        }

        WebsitePage websitePage =
                resolveWebsitePage(
                        navigationItem.getWebsitePage(),
                        navigationItem.getDestinationType()
                );

        String destinationUrl =
                validateAndNormalizeDestination(
                        navigationItem.getDestinationType(),
                        navigationItem.getDestinationUrl(),
                        websitePage
                );

        WebsiteNavigationItem itemToCreate =
                WebsiteNavigationItem.builder()
                        .websitePage(websitePage)
                        .navigationLocation(location)
                        .itemKey(itemKey)
                        .label(
                                normalizeRequired(
                                        navigationItem.getLabel(),
                                        "Navigation label"
                                )
                        )
                        .destinationType(
                                navigationItem.getDestinationType()
                        )
                        .destinationUrl(destinationUrl)
                        .targetBehavior(
                                navigationItem.getTargetBehavior()
                        )
                        .iconKey(
                                normalizeIconKey(
                                        navigationItem.getIconKey()
                                )
                        )
                        .displayOrder(
                                navigationItem.getDisplayOrder()
                        )
                        .isVisible(
                                navigationItem.getIsVisible()
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        return saveNavigationItem(
                itemToCreate,
                "Unable to create the navigation item because the "
                        + "location and item key are already in use."
        );
    }

    @Override
    @Transactional
    public WebsiteNavigationItem updateNavigationItem(
            UUID navigationItemId,
            WebsiteNavigationItem requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                navigationItemId,
                "Navigation item ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated navigation-item information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteNavigationItem existingItem =
                getNavigationItemForUpdate(
                        navigationItemId
                );

        validateEditableFields(requestedUpdate);

        String itemKey =
                normalizeItemKey(
                        requestedUpdate.getItemKey()
                );

        if (
                websiteNavigationItemRepository
                        .existsByNavigationLocationAndItemKeyAndNavigationItemIdNot(
                                requestedUpdate
                                        .getNavigationLocation(),
                                itemKey,
                                navigationItemId
                        )
        ) {
            throw conflict(
                    "Another navigation item already uses this "
                            + "location and item key."
            );
        }

        WebsitePage websitePage =
                resolveWebsitePage(
                        requestedUpdate.getWebsitePage(),
                        requestedUpdate.getDestinationType()
                );

        String destinationUrl =
                validateAndNormalizeDestination(
                        requestedUpdate.getDestinationType(),
                        requestedUpdate.getDestinationUrl(),
                        websitePage
                );

        existingItem.updateDetails(
                websitePage,
                requestedUpdate.getNavigationLocation(),
                itemKey,
                normalizeRequired(
                        requestedUpdate.getLabel(),
                        "Navigation label"
                ),
                requestedUpdate.getDestinationType(),
                destinationUrl,
                requestedUpdate.getTargetBehavior(),
                normalizeIconKey(
                        requestedUpdate.getIconKey()
                ),
                requestedUpdate.getDisplayOrder(),
                requestedUpdate.getIsVisible(),
                administrator
        );

        return saveNavigationItem(
                existingItem,
                "Unable to update the navigation item because the "
                        + "location and item key are already in use."
        );
    }

    @Override
    public WebsiteNavigationItem getNavigationItem(
            UUID navigationItemId
    ) {
        requireIdentifier(
                navigationItemId,
                "Navigation item ID"
        );

        return websiteNavigationItemRepository
                .findByNavigationItemIdAndDeletedAtIsNull(
                        navigationItemId
                )
                .orElseThrow(() -> notFound(
                        "Website navigation item was not found."
                ));
    }

    @Override
    public WebsiteNavigationItem
    getNavigationItemByLocationAndKey(
            WebsiteNavigationLocation navigationLocation,
            String itemKey
    ) {
        requireNavigationLocation(navigationLocation);

        return websiteNavigationItemRepository
                .findByNavigationLocationAndItemKeyAndDeletedAtIsNull(
                        navigationLocation,
                        normalizeItemKey(itemKey)
                )
                .orElseThrow(() -> notFound(
                        "Website navigation item was not found."
                ));
    }

    @Override
    public Page<WebsiteNavigationItem> searchNavigationItems(
            String keyword,
            WebsiteNavigationLocation navigationLocation,
            WebsiteNavigationDestinationType destinationType,
            Boolean isVisible,
            Pageable pageable
    ) {
        requirePageable(pageable);

        return websiteNavigationItemRepository
                .searchNavigationItems(
                        normalizeOptional(keyword),
                        navigationLocation,
                        destinationType,
                        isVisible,
                        pageable
                );
    }

    @Override
    public Page<WebsiteNavigationItem>
    getDeletedNavigationItems(
            Pageable pageable
    ) {
        requirePageable(pageable);

        return websiteNavigationItemRepository
                .findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(
                        pageable
                );
    }

    @Override
    public List<WebsiteNavigationItem>
    getPublicNavigationItems(
            WebsiteNavigationLocation navigationLocation
    ) {
        requireNavigationLocation(navigationLocation);

        return websiteNavigationItemRepository
                .findAllByNavigationLocationAndIsVisibleTrueAndDeletedAtIsNullOrderByDisplayOrderAscLabelAsc(
                        navigationLocation
                )
                .stream()
                .filter(
                        WebsiteNavigationItem
                                ::isPubliclyAvailable
                )
                .toList();
    }

    @Override
    public List<WebsiteNavigationItem>
    getAllPublicNavigationItems() {
        return websiteNavigationItemRepository
                .findAllByIsVisibleTrueAndDeletedAtIsNullOrderByNavigationLocationAscDisplayOrderAscLabelAsc()
                .stream()
                .filter(
                        WebsiteNavigationItem
                                ::isPubliclyAvailable
                )
                .toList();
    }

    @Override
    @Transactional
    public WebsiteNavigationItem
    updateNavigationItemVisibility(
            UUID navigationItemId,
            boolean isVisible,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteNavigationItem item =
                getNavigationItemForUpdate(
                        navigationItemId
                );

        if (isVisible) {
            validatePublicDestination(item);
        }

        item.updateVisibility(
                isVisible,
                administrator
        );

        return websiteNavigationItemRepository.save(
                item
        );
    }

    @Override
    @Transactional
    public void deleteNavigationItem(
            UUID navigationItemId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteNavigationItem item =
                getNavigationItemForUpdate(
                        navigationItemId
                );

        item.softDelete(administrator);

        websiteNavigationItemRepository.save(item);
    }

    @Override
    @Transactional
    public WebsiteNavigationItem restoreNavigationItem(
            UUID navigationItemId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteNavigationItem item =
                getNavigationItemIncludingDeletedForUpdate(
                        navigationItemId
                );

        if (!item.isDeleted()) {
            throw conflict(
                    "The website navigation item is not deleted."
            );
        }

        item.restore(administrator);

        return websiteNavigationItemRepository.save(
                item
        );
    }

    @Override
    public long countVisibleNavigationItems(
            WebsiteNavigationLocation navigationLocation
    ) {
        requireNavigationLocation(navigationLocation);

        return websiteNavigationItemRepository
                .countByNavigationLocationAndIsVisibleTrueAndDeletedAtIsNull(
                        navigationLocation
                );
    }

    @Override
    public long countDeletedNavigationItems() {
        return websiteNavigationItemRepository
                .countByDeletedAtIsNotNull();
    }

    private WebsiteNavigationItem
    getNavigationItemForUpdate(
            UUID navigationItemId
    ) {
        requireIdentifier(
                navigationItemId,
                "Navigation item ID"
        );

        return websiteNavigationItemRepository
                .findByIdForUpdate(navigationItemId)
                .orElseThrow(() -> notFound(
                        "Website navigation item was not found."
                ));
    }

    private WebsiteNavigationItem
    getNavigationItemIncludingDeletedForUpdate(
            UUID navigationItemId
    ) {
        requireIdentifier(
                navigationItemId,
                "Navigation item ID"
        );

        return websiteNavigationItemRepository
                .findIncludingDeletedByIdForUpdate(
                        navigationItemId
                )
                .orElseThrow(() -> notFound(
                        "Website navigation item was not found."
                ));
    }

    private WebsitePage resolveWebsitePage(
            WebsitePage requestedPage,
            WebsiteNavigationDestinationType destinationType
    ) {
        if (
                requestedPage == null
                        || requestedPage
                        .getWebsitePageId() == null
        ) {
            return null;
        }

        WebsitePage page =
                websitePageRepository
                        .findByWebsitePageIdAndDeletedAtIsNull(
                                requestedPage
                                        .getWebsitePageId()
                        )
                        .orElseThrow(() -> notFound(
                                "Referenced website page was not found."
                        ));

        if (
                destinationType
                        != WebsiteNavigationDestinationType
                        .INTERNAL_ROUTE
        ) {
            throw badRequest(
                    "A website page may only be assigned to an "
                            + "INTERNAL_ROUTE navigation item."
            );
        }

        return page;
    }

    private void validateEditableFields(
            WebsiteNavigationItem item
    ) {
        if (item.getNavigationLocation() == null) {
            throw badRequest(
                    "Navigation location is required."
            );
        }

        validateLength(
                item.getItemKey(),
                120,
                "Navigation item key",
                true
        );

        validateLength(
                item.getLabel(),
                180,
                "Navigation label",
                true
        );

        if (item.getDestinationType() == null) {
            throw badRequest(
                    "Navigation destination type is required."
            );
        }

        if (item.getTargetBehavior() == null) {
            throw badRequest(
                    "Navigation target behavior is required."
            );
        }

        validateLength(
                item.getDestinationUrl(),
                1500,
                "Destination URL",
                false
        );

        validateLength(
                item.getIconKey(),
                100,
                "Icon key",
                false
        );

        if (
                item.getDisplayOrder() == null
                        || item.getDisplayOrder() < 0
        ) {
            throw badRequest(
                    "Display order must not be negative."
            );
        }

        if (item.getIsVisible() == null) {
            throw badRequest(
                    "Visibility status is required."
            );
        }
    }

    private String validateAndNormalizeDestination(
            WebsiteNavigationDestinationType destinationType,
            String destinationUrl,
            WebsitePage websitePage
    ) {
        return switch (destinationType) {
            case INTERNAL_ROUTE ->
                    normalizeInternalRoute(
                            destinationUrl,
                            websitePage
                    );

            case EXTERNAL_URL ->
                    normalizeExternalUrl(destinationUrl);

            case ANCHOR ->
                    normalizeAnchor(destinationUrl);

            case ACTION ->
                    normalizeAction(destinationUrl);
        };
    }

    private String normalizeInternalRoute(
            String destinationUrl,
            WebsitePage websitePage
    ) {
        if (websitePage != null) {
            return normalizeOptional(destinationUrl);
        }

        String normalized =
                normalizeRequired(
                        destinationUrl,
                        "Internal route"
                );

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

        return normalized;
    }

    private String normalizeExternalUrl(
            String destinationUrl
    ) {
        String normalized =
                normalizeRequired(
                        destinationUrl,
                        "External destination URL"
                );

        try {
            URI uri = new URI(normalized);

            String scheme = uri.getScheme();
            String host = uri.getHost();

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
                        "External destination must be a valid HTTP "
                                + "or HTTPS URL."
                );
            }

            return uri.normalize().toString();
        } catch (URISyntaxException exception) {
            throw badRequest(
                    "External destination must be a valid URL."
            );
        }
    }

    private String normalizeAnchor(
            String destinationUrl
    ) {
        String normalized =
                normalizeRequired(
                        destinationUrl,
                        "Anchor destination"
                );

        if (!normalized.contains("#")) {
            throw badRequest(
                    "Anchor destinations must contain a # fragment."
            );
        }

        return normalized;
    }

    private String normalizeAction(
            String destinationUrl
    ) {
        String normalized =
                normalizeRequired(
                        destinationUrl,
                        "Navigation action"
                )
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    "Navigation action is required."
            );
        }

        return normalized;
    }

    private void validatePublicDestination(
            WebsiteNavigationItem item
    ) {
        if (
                item.getDestinationType()
                        == WebsiteNavigationDestinationType
                        .INTERNAL_ROUTE
                        && item.getWebsitePage() != null
        ) {
            WebsitePage page = item.getWebsitePage();

            if (
                    page.isDeleted()
                            || !Boolean.TRUE.equals(
                            page.getIsActive()
                    )
            ) {
                throw conflict(
                        "The navigation item cannot be made visible "
                                + "because its website page is inactive "
                                + "or deleted."
                );
            }
        }

        if (item.resolveDestination() == null) {
            throw conflict(
                    "The navigation item cannot be made visible "
                            + "without a valid destination."
            );
        }
    }

    private String normalizeItemKey(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Navigation item key"
                )
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    "Navigation item key is required."
            );
        }

        if (normalized.length() > 120) {
            throw badRequest(
                    "Navigation item key must not exceed "
                            + "120 characters."
            );
        }

        return normalized;
    }

    private String normalizeIconKey(
            String value
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            return null;
        }

        normalized = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (normalized.isBlank()) {
            return null;
        }

        if (normalized.length() > 100) {
            throw badRequest(
                    "Icon key must not exceed 100 characters."
            );
        }

        return normalized;
    }

    private WebsiteNavigationItem saveNavigationItem(
            WebsiteNavigationItem item,
            String conflictMessage
    ) {
        try {
            return websiteNavigationItemRepository
                    .saveAndFlush(item);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    conflictMessage,
                    exception
            );
        }
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

    private void requireNavigationLocation(
            WebsiteNavigationLocation navigationLocation
    ) {
        if (navigationLocation == null) {
            throw badRequest(
                    "Navigation location is required."
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

    private void validateLength(
            String value,
            int maximumLength,
            String fieldName,
            boolean required
    ) {
        String normalized = normalizeOptional(value);

        if (required && normalized == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }

        if (
                normalized != null
                        && normalized.length() > maximumLength
        ) {
            throw badRequest(
                    fieldName
                            + " must not exceed "
                            + maximumLength
                            + " characters."
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