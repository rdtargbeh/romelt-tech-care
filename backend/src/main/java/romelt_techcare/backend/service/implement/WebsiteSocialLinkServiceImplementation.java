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
import romelt_techcare.backend.entity.WebsiteSocialLink;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteSocialLinkRepository;
import romelt_techcare.backend.service.WebsiteSocialLinkService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SOCIAL LINK SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production business rules for social and external-profile
 * links displayed on the Romelt TechCare website.
 *
 * Responsibilities:
 * - Creates and updates social links.
 * - Normalizes stable platform and icon keys.
 * - Enforces one record per normalized platform.
 * - Validates profile URLs.
 * - Supports administrator search and public retrieval.
 * - Activates, deactivates, and deletes links.
 * - Records the administrator responsible for each write.
 *
 * Publishing behavior:
 * Active links become publicly available immediately after persistence.
 *
 * Deletion behavior:
 * The supplied schema has no soft-delete columns. Deletion therefore
 * physically removes the social-link record.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteSocialLinkServiceImplementation
        implements WebsiteSocialLinkService {

    private final WebsiteSocialLinkRepository
            websiteSocialLinkRepository;

    private final AdminUserRepository adminUserRepository;

    /**
     * Creates a new social link.
     */
    @Override
    @Transactional
    public WebsiteSocialLink createSocialLink(
            WebsiteSocialLink socialLink,
            UUID administratorId
    ) {
        if (socialLink == null) {
            throw badRequest(
                    "Website social-link information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        String normalizedPlatform =
                normalizePlatform(socialLink.getPlatform());

        validatePlatformAvailability(
                normalizedPlatform,
                null
        );

        validateEditableFields(socialLink);

        WebsiteSocialLink socialLinkToCreate =
                WebsiteSocialLink.builder()
                        .platform(normalizedPlatform)
                        .label(socialLink.getLabel())
                        .profileUrl(
                                normalizeAndValidateUrl(
                                        socialLink.getProfileUrl()
                                )
                        )
                        .iconKey(
                                normalizeIconKey(
                                        socialLink.getIconKey()
                                )
                        )
                        .displayOrder(
                                socialLink.getDisplayOrder() == null
                                        ? 0
                                        : socialLink.getDisplayOrder()
                        )
                        .isActive(
                                socialLink.getIsActive() == null
                                        || socialLink.getIsActive()
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        return saveSocialLink(
                socialLinkToCreate,
                "A website social link already exists for this platform."
        );
    }

    /**
     * Updates an existing social link.
     */
    @Override
    @Transactional
    public WebsiteSocialLink updateSocialLink(
            UUID socialLinkId,
            WebsiteSocialLink requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                socialLinkId,
                "Social link ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated website social-link information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteSocialLink existingSocialLink =
                getSocialLinkForUpdate(socialLinkId);

        String normalizedPlatform =
                normalizePlatform(
                        requestedUpdate.getPlatform()
                );

        validatePlatformAvailability(
                normalizedPlatform,
                socialLinkId
        );

        validateEditableFields(requestedUpdate);

        existingSocialLink.updateDetails(
                normalizedPlatform,
                normalizeOptional(requestedUpdate.getLabel()),
                normalizeAndValidateUrl(
                        requestedUpdate.getProfileUrl()
                ),
                normalizeIconKey(
                        requestedUpdate.getIconKey()
                ),
                requestedUpdate.getDisplayOrder(),
                requestedUpdate.getIsActive(),
                administrator
        );

        return saveSocialLink(
                existingSocialLink,
                "Another website social link already uses this platform."
        );
    }

    /**
     * Retrieves one social link.
     */
    @Override
    public WebsiteSocialLink getSocialLink(
            UUID socialLinkId
    ) {
        requireIdentifier(
                socialLinkId,
                "Social link ID"
        );

        return websiteSocialLinkRepository
                .findBySocialLinkId(socialLinkId)
                .orElseThrow(() -> notFound(
                        "Website social link was not found."
                ));
    }

    /**
     * Retrieves one link by normalized platform.
     */
    @Override
    public WebsiteSocialLink getSocialLinkByPlatform(
            String platform
    ) {
        String normalizedPlatform =
                normalizePlatform(platform);

        return websiteSocialLinkRepository
                .findByPlatform(normalizedPlatform)
                .orElseThrow(() -> notFound(
                        "Website social link was not found."
                ));
    }

    /**
     * Searches administrator-facing records.
     */
    @Override
    public Page<WebsiteSocialLink> searchSocialLinks(
            String keyword,
            Boolean isActive,
            Pageable pageable
    ) {
        requirePageable(pageable);

        return websiteSocialLinkRepository
                .searchSocialLinks(
                        normalizeOptional(keyword),
                        isActive,
                        pageable
                );
    }

    /**
     * Retrieves active public links.
     */
    @Override
    public List<WebsiteSocialLink>
    getActivePublicSocialLinks() {
        return websiteSocialLinkRepository
                .findAllByIsActiveTrueOrderByDisplayOrderAscPlatformAsc();
    }

    /**
     * Updates active status.
     */
    @Override
    @Transactional
    public WebsiteSocialLink updateSocialLinkStatus(
            UUID socialLinkId,
            boolean isActive,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteSocialLink socialLink =
                getSocialLinkForUpdate(socialLinkId);

        if (isActive) {
            socialLink.activate(administrator);
        } else {
            socialLink.deactivate(administrator);
        }

        return websiteSocialLinkRepository.save(
                socialLink
        );
    }

    /**
     * Activates a social link.
     */
    @Override
    @Transactional
    public WebsiteSocialLink activateSocialLink(
            UUID socialLinkId,
            UUID administratorId
    ) {
        return updateSocialLinkStatus(
                socialLinkId,
                true,
                administratorId
        );
    }

    /**
     * Deactivates a social link.
     */
    @Override
    @Transactional
    public WebsiteSocialLink deactivateSocialLink(
            UUID socialLinkId,
            UUID administratorId
    ) {
        return updateSocialLinkStatus(
                socialLinkId,
                false,
                administratorId
        );
    }

    /**
     * Permanently deletes a social link.
     */
    @Override
    @Transactional
    public void deleteSocialLink(
            UUID socialLinkId,
            UUID administratorId
    ) {
        getRequiredAdministrator(administratorId);

        WebsiteSocialLink socialLink =
                getSocialLinkForUpdate(socialLinkId);

        websiteSocialLinkRepository.delete(
                socialLink
        );
    }

    /**
     * Counts active links.
     */
    @Override
    public long countActiveSocialLinks() {
        return websiteSocialLinkRepository
                .countByIsActiveTrue();
    }

    /**
     * Retrieves and locks one social link for writing.
     */
    private WebsiteSocialLink getSocialLinkForUpdate(
            UUID socialLinkId
    ) {
        requireIdentifier(
                socialLinkId,
                "Social link ID"
        );

        return websiteSocialLinkRepository
                .findByIdForUpdate(socialLinkId)
                .orElseThrow(() -> notFound(
                        "Website social link was not found."
                ));
    }

    /**
     * Ensures the normalized platform is unique.
     */
    private void validatePlatformAvailability(
            String platform,
            UUID currentSocialLinkId
    ) {
        boolean exists;

        if (currentSocialLinkId == null) {
            exists = websiteSocialLinkRepository
                    .existsByPlatform(platform);
        } else {
            exists = websiteSocialLinkRepository
                    .existsByPlatformAndSocialLinkIdNot(
                            platform,
                            currentSocialLinkId
                    );
        }

        if (exists) {
            throw conflict(
                    "A website social link already exists for this platform."
            );
        }
    }

    /**
     * Validates editable input.
     */
    private void validateEditableFields(
            WebsiteSocialLink socialLink
    ) {
        if (isBlank(socialLink.getPlatform())) {
            throw badRequest(
                    "Platform is required."
            );
        }

        if (
                socialLink.getPlatform().trim().length() > 50
        ) {
            throw badRequest(
                    "Platform must not exceed 50 characters."
            );
        }

        if (isBlank(socialLink.getProfileUrl())) {
            throw badRequest(
                    "Profile URL is required."
            );
        }

        if (
                socialLink.getProfileUrl().trim().length() > 1000
        ) {
            throw badRequest(
                    "Profile URL must not exceed 1000 characters."
            );
        }

        if (
                socialLink.getLabel() != null
                        && socialLink.getLabel().trim().length() > 100
        ) {
            throw badRequest(
                    "Label must not exceed 100 characters."
            );
        }

        if (
                socialLink.getIconKey() != null
                        && socialLink.getIconKey().trim().length() > 100
        ) {
            throw badRequest(
                    "Icon key must not exceed 100 characters."
            );
        }

        if (
                socialLink.getDisplayOrder() == null
                        || socialLink.getDisplayOrder() < 0
        ) {
            throw badRequest(
                    "Display order must not be negative."
            );
        }

        if (socialLink.getIsActive() == null) {
            throw badRequest(
                    "Active status is required."
            );
        }

        normalizeAndValidateUrl(
                socialLink.getProfileUrl()
        );
    }

    /**
     * Normalizes and validates a public HTTP or HTTPS URL.
     */
    private String normalizeAndValidateUrl(
            String value
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw badRequest(
                    "Profile URL is required."
            );
        }

        try {
            URI uri = new URI(normalized);

            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (
                    scheme == null
                            || host == null
                            || (
                            !"http".equalsIgnoreCase(scheme)
                                    && !"https".equalsIgnoreCase(scheme)
                    )
            ) {
                throw badRequest(
                        "Profile URL must be a valid HTTP or HTTPS URL."
                );
            }

            return uri.normalize().toString();
        } catch (URISyntaxException exception) {
            throw badRequest(
                    "Profile URL must be a valid URL."
            );
        }
    }

    /**
     * Normalizes the stable platform key.
     */
    private String normalizePlatform(
            String value
    ) {
        String normalized = normalizeOptional(value);

        if (normalized == null) {
            throw badRequest(
                    "Platform is required."
            );
        }

        normalized = normalized
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    "Platform is required."
            );
        }

        if (normalized.length() > 50) {
            throw badRequest(
                    "Normalized platform must not exceed 50 characters."
            );
        }

        return normalized;
    }

    /**
     * Normalizes a frontend icon key.
     */
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

    /**
     * Retrieves the administrator responsible for a write operation.
     */
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

    private WebsiteSocialLink saveSocialLink(
            WebsiteSocialLink socialLink,
            String conflictMessage
    ) {
        try {
            return websiteSocialLinkRepository
                    .saveAndFlush(socialLink);
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