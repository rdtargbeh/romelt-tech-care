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
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.entity.WebsiteService;
import romelt_techcare.backend.entity.WebsiteServiceVersion;
import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;
import romelt_techcare.backend.enums.WebsiteMediaUsageResourceType;
import romelt_techcare.backend.enums.WebsiteServiceStatus;
import romelt_techcare.backend.enums.WebsiteServiceVersionStatus;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteMediaAssetRepository;
import romelt_techcare.backend.repository.WebsiteServiceRepository;
import romelt_techcare.backend.repository.WebsiteServiceVersionRepository;
import romelt_techcare.backend.service.WebsiteMediaUsageService;
import romelt_techcare.backend.service.WebsiteServiceVersionService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — SERVICE VERSION SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements production draft, publication, archive, history, media,
 * and public-retrieval rules for website service versions.
 *
 * Publication transaction:
 * 1. Lock the owning stable service.
 * 2. Lock the selected draft.
 * 3. Archive the current published version, when present.
 * 4. Publish the selected draft.
 * 5. Clear the stable service's draft pointer.
 * 6. Assign its published pointer.
 *
 * Media tracking:
 * Card and hero images are registered in WebsiteMediaUsage using the
 * SERVICE_VERSION resource type.
 *
 * Public behavior:
 * Public queries return only versions that:
 * - belong to active, non-deleted services;
 * - are the service's current published version;
 * - have PUBLISHED status;
 * - are marked public;
 * - are inside their optional effective window.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteServiceVersionServiceImplementation
        implements WebsiteServiceVersionService {

    private static final String CARD_IMAGE_USAGE_FIELD =
            "CARD_IMAGE";

    private static final String HERO_IMAGE_USAGE_FIELD =
            "HERO_IMAGE";

    private final WebsiteServiceVersionRepository
            websiteServiceVersionRepository;

    private final WebsiteServiceRepository
            websiteServiceRepository;

    private final WebsiteMediaAssetRepository
            websiteMediaAssetRepository;

    private final WebsiteMediaUsageService
            websiteMediaUsageService;

    private final AdminUserRepository adminUserRepository;

    @Override
    @Transactional
    public WebsiteServiceVersion createDraft(
            UUID serviceId,
            WebsiteServiceVersion requestedDraft,
            UUID administratorId
    ) {
        requireIdentifier(
                serviceId,
                "Service ID"
        );

        if (requestedDraft == null) {
            throw badRequest(
                    "Service draft information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteService service =
                getServiceForUpdate(serviceId);

        ensureServiceUsable(service);
        ensureNoExistingDraft(serviceId);

        validateDraftFields(requestedDraft);

        int nextVersionNumber =
                websiteServiceVersionRepository
                        .findMaximumVersionNumber(serviceId)
                        + 1;

        WebsiteMediaAsset cardImage =
                validatePublicImage(
                        requestedDraft.getCardImageMedia(),
                        "Card image"
                );

        WebsiteMediaAsset heroImage =
                validatePublicImage(
                        requestedDraft.getHeroImageMedia(),
                        "Hero image"
                );

        WebsiteServiceVersion draft =
                WebsiteServiceVersion.builder()
                        .websiteService(service)
                        .versionNumber(nextVersionNumber)
                        .versionStatus(
                                WebsiteServiceVersionStatus.DRAFT
                        )
                        .serviceName(
                                normalizeRequired(
                                        requestedDraft.getServiceName(),
                                        "Service name"
                                )
                        )
                        .shortDescription(
                                normalizeOptional(
                                        requestedDraft
                                                .getShortDescription()
                                )
                        )
                        .fullDescription(
                                normalizeOptional(
                                        requestedDraft
                                                .getFullDescription()
                                )
                        )
                        .iconKey(
                                normalizeIconKey(
                                        requestedDraft.getIconKey()
                                )
                        )
                        .cardImageMedia(cardImage)
                        .heroImageMedia(heroImage)
                        .startingPrice(
                                requestedDraft.getStartingPrice()
                        )
                        .currencyCode(
                                normalizeCurrencyCode(
                                        requestedDraft
                                                .getCurrencyCode()
                                )
                        )
                        .priceUnitLabel(
                                normalizeOptional(
                                        requestedDraft
                                                .getPriceUnitLabel()
                                )
                        )
                        .displayOrder(
                                requestedDraft.getDisplayOrder()
                        )
                        .isFeatured(
                                requestedDraft.getIsFeatured()
                        )
                        .isBookable(
                                requestedDraft.getIsBookable()
                        )
                        .isPublic(
                                requestedDraft.getIsPublic()
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
                        .effectiveFrom(
                                requestedDraft.getEffectiveFrom()
                        )
                        .effectiveUntil(
                                requestedDraft.getEffectiveUntil()
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

        WebsiteServiceVersion savedDraft =
                saveVersion(
                        draft,
                        "Unable to create the service draft. The service "
                                + "may already have a current draft."
                );

        service.assignDraftVersion(
                savedDraft.getServiceVersionId(),
                administrator
        );

        websiteServiceRepository.saveAndFlush(service);

        synchronizeMediaUsage(savedDraft);

        return getServiceVersion(
                savedDraft.getServiceVersionId()
        );
    }

    @Override
    @Transactional
    public WebsiteServiceVersion createDraftFromPublishedVersion(
            UUID serviceId,
            String changeSummary,
            UUID administratorId
    ) {
        requireIdentifier(
                serviceId,
                "Service ID"
        );

        validateLength(
                changeSummary,
                1000,
                "Change summary"
        );

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteService service =
                getServiceForUpdate(serviceId);

        ensureServiceUsable(service);
        ensureNoExistingDraft(serviceId);

        WebsiteServiceVersion publishedVersion =
                websiteServiceVersionRepository
                        .findByServiceAndStatusForUpdate(
                                serviceId,
                                WebsiteServiceVersionStatus.PUBLISHED
                        )
                        .orElseThrow(() -> conflict(
                                "The website service has no published "
                                        + "version to copy."
                        ));

        int nextVersionNumber =
                websiteServiceVersionRepository
                        .findMaximumVersionNumber(serviceId)
                        + 1;

        WebsiteServiceVersion draft =
                WebsiteServiceVersion.builder()
                        .websiteService(service)
                        .versionNumber(nextVersionNumber)
                        .versionStatus(
                                WebsiteServiceVersionStatus.DRAFT
                        )
                        .serviceName(
                                publishedVersion.getServiceName()
                        )
                        .shortDescription(
                                publishedVersion
                                        .getShortDescription()
                        )
                        .fullDescription(
                                publishedVersion
                                        .getFullDescription()
                        )
                        .iconKey(
                                publishedVersion.getIconKey()
                        )
                        .cardImageMedia(
                                publishedVersion
                                        .getCardImageMedia()
                        )
                        .heroImageMedia(
                                publishedVersion
                                        .getHeroImageMedia()
                        )
                        .startingPrice(
                                publishedVersion
                                        .getStartingPrice()
                        )
                        .currencyCode(
                                publishedVersion
                                        .getCurrencyCode()
                        )
                        .priceUnitLabel(
                                publishedVersion
                                        .getPriceUnitLabel()
                        )
                        .displayOrder(
                                publishedVersion
                                        .getDisplayOrder()
                        )
                        .isFeatured(
                                publishedVersion
                                        .getIsFeatured()
                        )
                        .isBookable(
                                publishedVersion
                                        .getIsBookable()
                        )
                        .isPublic(
                                publishedVersion.getIsPublic()
                        )
                        .seoTitle(
                                publishedVersion.getSeoTitle()
                        )
                        .seoDescription(
                                publishedVersion
                                        .getSeoDescription()
                        )
                        .effectiveFrom(
                                publishedVersion
                                        .getEffectiveFrom()
                        )
                        .effectiveUntil(
                                publishedVersion
                                        .getEffectiveUntil()
                        )
                        .changeSummary(
                                normalizeOptional(changeSummary)
                        )
                        .createdByAdminUser(administrator)
                        .updatedByAdminUser(administrator)
                        .build();

        WebsiteServiceVersion savedDraft =
                saveVersion(
                        draft,
                        "Unable to create a draft from the published "
                                + "service version."
                );

        service.assignDraftVersion(
                savedDraft.getServiceVersionId(),
                administrator
        );

        websiteServiceRepository.saveAndFlush(service);

        synchronizeMediaUsage(savedDraft);

        return getServiceVersion(
                savedDraft.getServiceVersionId()
        );
    }

    @Override
    @Transactional
    public WebsiteServiceVersion updateDraft(
            UUID serviceVersionId,
            WebsiteServiceVersion requestedUpdate,
            UUID administratorId
    ) {
        requireIdentifier(
                serviceVersionId,
                "Service version ID"
        );

        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated service draft information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteServiceVersion draft =
                getVersionForUpdate(serviceVersionId);

        if (!draft.isDraft()) {
            throw conflict(
                    "Only a draft service version may be edited."
            );
        }

        WebsiteService service =
                draft.getWebsiteService();

        ensureServiceUsable(service);
        validateDraftFields(requestedUpdate);

        WebsiteMediaAsset cardImage =
                validatePublicImage(
                        requestedUpdate.getCardImageMedia(),
                        "Card image"
                );

        WebsiteMediaAsset heroImage =
                validatePublicImage(
                        requestedUpdate.getHeroImageMedia(),
                        "Hero image"
                );

        draft.updateDraft(
                normalizeRequired(
                        requestedUpdate.getServiceName(),
                        "Service name"
                ),
                normalizeOptional(
                        requestedUpdate.getShortDescription()
                ),
                normalizeOptional(
                        requestedUpdate.getFullDescription()
                ),
                normalizeIconKey(
                        requestedUpdate.getIconKey()
                ),
                cardImage,
                heroImage,
                requestedUpdate.getStartingPrice(),
                normalizeCurrencyCode(
                        requestedUpdate.getCurrencyCode()
                ),
                normalizeOptional(
                        requestedUpdate.getPriceUnitLabel()
                ),
                requestedUpdate.getDisplayOrder(),
                requestedUpdate.getIsFeatured(),
                requestedUpdate.getIsBookable(),
                requestedUpdate.getIsPublic(),
                normalizeOptional(
                        requestedUpdate.getSeoTitle()
                ),
                normalizeOptional(
                        requestedUpdate.getSeoDescription()
                ),
                requestedUpdate.getEffectiveFrom(),
                requestedUpdate.getEffectiveUntil(),
                normalizeOptional(
                        requestedUpdate.getChangeSummary()
                ),
                administrator
        );

        WebsiteServiceVersion savedDraft =
                saveVersion(
                        draft,
                        "Unable to update the service draft."
                );

        synchronizeMediaUsage(savedDraft);

        return getServiceVersion(serviceVersionId);
    }

    @Override
    public WebsiteServiceVersion getServiceVersion(
            UUID serviceVersionId
    ) {
        requireIdentifier(
                serviceVersionId,
                "Service version ID"
        );

        return websiteServiceVersionRepository
                .findByServiceVersionId(serviceVersionId)
                .orElseThrow(() -> notFound(
                        "Website service version was not found."
                ));
    }

    @Override
    public WebsiteServiceVersion getCurrentDraft(
            UUID serviceId
    ) {
        requireIdentifier(
                serviceId,
                "Service ID"
        );

        return websiteServiceVersionRepository
                .findByWebsiteService_ServiceIdAndVersionStatus(
                        serviceId,
                        WebsiteServiceVersionStatus.DRAFT
                )
                .orElseThrow(() -> notFound(
                        "The website service has no current draft."
                ));
    }

    @Override
    public WebsiteServiceVersion getCurrentPublishedVersion(
            UUID serviceId
    ) {
        requireIdentifier(
                serviceId,
                "Service ID"
        );

        return websiteServiceVersionRepository
                .findByWebsiteService_ServiceIdAndVersionStatus(
                        serviceId,
                        WebsiteServiceVersionStatus.PUBLISHED
                )
                .orElseThrow(() -> notFound(
                        "The website service has no published version."
                ));
    }

    @Override
    public Page<WebsiteServiceVersion> getVersionHistory(
            UUID serviceId,
            WebsiteServiceVersionStatus versionStatus,
            Pageable pageable
    ) {
        requireIdentifier(
                serviceId,
                "Service ID"
        );

        requirePageable(pageable);

        if (!websiteServiceRepository.existsById(serviceId)) {
            throw notFound(
                    "Website service was not found."
            );
        }

        if (versionStatus == null) {
            return websiteServiceVersionRepository
                    .findAllByWebsiteService_ServiceIdOrderByVersionNumberDesc(
                            serviceId,
                            pageable
                    );
        }

        return websiteServiceVersionRepository
                .findAllByWebsiteService_ServiceIdAndVersionStatusOrderByVersionNumberDesc(
                        serviceId,
                        versionStatus,
                        pageable
                );
    }

    @Override
    @Transactional
    public WebsiteServiceVersion publishDraft(
            UUID serviceVersionId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteServiceVersion draft =
                getVersionForUpdate(serviceVersionId);

        if (!draft.isDraft()) {
            throw conflict(
                    "Only a draft service version may be published."
            );
        }

        WebsiteService service =
                getServiceForUpdate(
                        draft.getWebsiteService()
                                .getServiceId()
                );

        ensureServiceUsable(service);
        validateDraftFields(draft);

        if (
                !serviceVersionId.equals(
                        service.getDraftVersionId()
                )
        ) {
            throw conflict(
                    "The selected version is not the service's "
                            + "current draft."
            );
        }

        WebsiteServiceVersion currentPublished =
                websiteServiceVersionRepository
                        .findByServiceAndStatusForUpdate(
                                service.getServiceId(),
                                WebsiteServiceVersionStatus.PUBLISHED
                        )
                        .orElse(null);

        if (currentPublished != null) {
            currentPublished.archive(administrator);

            websiteServiceVersionRepository.saveAndFlush(
                    currentPublished
            );
        }

        draft.publish(administrator);

        WebsiteServiceVersion publishedDraft =
                websiteServiceVersionRepository
                        .saveAndFlush(draft);

        service.assignPublishedVersion(
                publishedDraft.getServiceVersionId(),
                administrator
        );

        service.clearDraftVersion(administrator);

        websiteServiceRepository.saveAndFlush(service);

        synchronizeMediaUsage(publishedDraft);

        return getServiceVersion(
                publishedDraft.getServiceVersionId()
        );
    }

    @Override
    @Transactional
    public WebsiteServiceVersion archiveVersion(
            UUID serviceVersionId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteServiceVersion version =
                getVersionForUpdate(serviceVersionId);

        WebsiteService service =
                getServiceForUpdate(
                        version.getWebsiteService()
                                .getServiceId()
                );

        if (version.isArchived()) {
            return getServiceVersion(serviceVersionId);
        }

        boolean wasDraft = version.isDraft();
        boolean wasPublished = version.isPublished();

        version.archive(administrator);

        websiteServiceVersionRepository.saveAndFlush(
                version
        );

        if (
                wasDraft
                        && serviceVersionId.equals(
                        service.getDraftVersionId()
                )
        ) {
            service.clearDraftVersion(administrator);
        }

        if (
                wasPublished
                        && serviceVersionId.equals(
                        service.getPublishedVersionId()
                )
        ) {
            service.clearPublishedVersion(administrator);
        }

        websiteServiceRepository.saveAndFlush(service);

        return getServiceVersion(serviceVersionId);
    }

    @Override
    @Transactional
    public void deleteDraft(
            UUID serviceVersionId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteServiceVersion draft =
                getVersionForUpdate(serviceVersionId);

        if (!draft.isDraft()) {
            throw conflict(
                    "Only a draft service version may be permanently deleted."
            );
        }

        WebsiteService service =
                getServiceForUpdate(
                        draft.getWebsiteService()
                                .getServiceId()
                );

        websiteMediaUsageService
                .removeAllUsagesForResource(
                        WebsiteMediaUsageResourceType
                                .SERVICE_VERSION,
                        serviceVersionId
                );

        if (
                serviceVersionId.equals(
                        service.getDraftVersionId()
                )
        ) {
            service.clearDraftVersion(administrator);
            websiteServiceRepository.saveAndFlush(service);
        }

        websiteServiceVersionRepository.delete(draft);
        websiteServiceVersionRepository.flush();
    }

    @Override
    public WebsiteServiceVersion getPublicServiceByCode(
            String serviceCode
    ) {
        return websiteServiceVersionRepository
                .findPublicByServiceCode(
                        normalizeServiceCode(serviceCode),
                        Instant.now()
                )
                .orElseThrow(() -> notFound(
                        "Public website service was not found."
                ));
    }

    @Override
    public WebsiteServiceVersion getPublicServiceBySlug(
            String serviceSlug
    ) {
        return websiteServiceVersionRepository
                .findPublicByServiceSlug(
                        normalizeServiceSlug(serviceSlug),
                        Instant.now()
                )
                .orElseThrow(() -> notFound(
                        "Public website service was not found."
                ));
    }

    @Override
    public List<WebsiteServiceVersion> getPublicServices() {
        return websiteServiceVersionRepository
                .findAllPublicServices(Instant.now());
    }

    @Override
    public List<WebsiteServiceVersion>
    getFeaturedPublicServices() {
        return websiteServiceVersionRepository
                .findAllFeaturedPublicServices(
                        Instant.now()
                );
    }

    @Override
    public List<WebsiteServiceVersion>
    getBookablePublicServices() {
        return websiteServiceVersionRepository
                .findAllBookablePublicServices(
                        Instant.now()
                );
    }

    private WebsiteServiceVersion getVersionForUpdate(
            UUID serviceVersionId
    ) {
        requireIdentifier(
                serviceVersionId,
                "Service version ID"
        );

        return websiteServiceVersionRepository
                .findByIdForUpdate(serviceVersionId)
                .orElseThrow(() -> notFound(
                        "Website service version was not found."
                ));
    }

    private WebsiteService getServiceForUpdate(
            UUID serviceId
    ) {
        requireIdentifier(
                serviceId,
                "Service ID"
        );

        return websiteServiceRepository
                .findByIdForUpdate(serviceId)
                .orElseThrow(() -> notFound(
                        "Website service was not found."
                ));
    }

    private void ensureNoExistingDraft(
            UUID serviceId
    ) {
        if (
                websiteServiceVersionRepository
                        .existsByWebsiteService_ServiceIdAndVersionStatus(
                                serviceId,
                                WebsiteServiceVersionStatus.DRAFT
                        )
        ) {
            throw conflict(
                    "The website service already has a current draft."
            );
        }
    }

    private void ensureServiceUsable(
            WebsiteService service
    ) {
        if (service == null || service.isDeleted()) {
            throw conflict(
                    "A deleted website service cannot manage content versions."
            );
        }

        if (
                service.getServiceStatus()
                        == WebsiteServiceStatus.ARCHIVED
        ) {
            throw conflict(
                    "An archived website service cannot manage new content versions."
            );
        }
    }

    private void validateDraftFields(
            WebsiteServiceVersion version
    ) {
        validateLength(
                version.getServiceName(),
                180,
                "Service name",
                true
        );

        validateLength(
                version.getShortDescription(),
                500,
                "Short description",
                false
        );

        validateLength(
                version.getIconKey(),
                100,
                "Icon key",
                false
        );

        validateLength(
                version.getPriceUnitLabel(),
                100,
                "Price unit label",
                false
        );

        validateLength(
                version.getSeoTitle(),
                255,
                "SEO title",
                false
        );

        validateLength(
                version.getSeoDescription(),
                500,
                "SEO description",
                false
        );

        validateLength(
                version.getChangeSummary(),
                1000,
                "Change summary",
                false
        );

        BigDecimal startingPrice =
                version.getStartingPrice();

        if (
                startingPrice != null
                        && startingPrice.compareTo(
                        BigDecimal.ZERO
                ) < 0
        ) {
            throw badRequest(
                    "Starting price must not be negative."
            );
        }

        normalizeCurrencyCode(
                version.getCurrencyCode()
        );

        if (
                version.getDisplayOrder() == null
                        || version.getDisplayOrder() < 0
        ) {
            throw badRequest(
                    "Display order must not be negative."
            );
        }

        if (version.getIsFeatured() == null) {
            throw badRequest(
                    "Featured status is required."
            );
        }

        if (version.getIsBookable() == null) {
            throw badRequest(
                    "Bookable status is required."
            );
        }

        if (version.getIsPublic() == null) {
            throw badRequest(
                    "Public status is required."
            );
        }

        if (
                version.getEffectiveUntil() != null
                        && version.getEffectiveFrom() != null
                        && !version
                        .getEffectiveUntil()
                        .isAfter(
                                version.getEffectiveFrom()
                        )
        ) {
            throw badRequest(
                    "Effective-until time must be after effective-from time."
            );
        }
    }

    private WebsiteMediaAsset validatePublicImage(
            WebsiteMediaAsset requestedMedia,
            String fieldName
    ) {
        if (
                requestedMedia == null
                        || requestedMedia.getMediaAssetId() == null
        ) {
            return null;
        }

        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetRepository
                        .findByMediaAssetIdAndDeletedAtIsNull(
                                requestedMedia.getMediaAssetId()
                        )
                        .orElseThrow(() -> notFound(
                                fieldName
                                        + " media asset was not found."
                        ));

        if (
                mediaAsset.getAssetStatus()
                        != WebsiteMediaAssetStatus.ACTIVE
        ) {
            throw conflict(
                    fieldName
                            + " must reference an active media asset."
            );
        }

        if (!Boolean.TRUE.equals(mediaAsset.getIsPublic())) {
            throw conflict(
                    fieldName
                            + " must reference a public media asset."
            );
        }

        if (!mediaAsset.isPubliclyAvailable()) {
            throw conflict(
                    fieldName
                            + " media asset is not publicly available."
            );
        }

        if (
                mediaAsset.getMimeType() == null
                        || !mediaAsset
                        .getMimeType()
                        .toLowerCase(Locale.ROOT)
                        .startsWith("image/")
        ) {
            throw conflict(
                    fieldName
                            + " must reference an image media asset."
            );
        }

        return mediaAsset;
    }

    private void synchronizeMediaUsage(
            WebsiteServiceVersion version
    ) {
        UUID cardImageMediaId =
                version.getCardImageMedia() == null
                        ? null
                        : version.getCardImageMedia()
                        .getMediaAssetId();

        UUID heroImageMediaId =
                version.getHeroImageMedia() == null
                        ? null
                        : version.getHeroImageMedia()
                        .getMediaAssetId();

        websiteMediaUsageService.replaceUsage(
                cardImageMediaId,
                WebsiteMediaUsageResourceType
                        .SERVICE_VERSION,
                version.getServiceVersionId(),
                CARD_IMAGE_USAGE_FIELD,
                "Card image for website service version "
                        + version.getVersionNumber()
                        + "."
        );

        websiteMediaUsageService.replaceUsage(
                heroImageMediaId,
                WebsiteMediaUsageResourceType
                        .SERVICE_VERSION,
                version.getServiceVersionId(),
                HERO_IMAGE_USAGE_FIELD,
                "Hero image for website service version "
                        + version.getVersionNumber()
                        + "."
        );
    }

    private String normalizeServiceCode(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Service code"
                )
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[^A-Z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    "Service code is required."
            );
        }

        if (normalized.length() > 100) {
            throw badRequest(
                    "Service code must not exceed 100 characters."
            );
        }

        return normalized;
    }

    private String normalizeServiceSlug(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Service slug"
                )
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-+|-+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    "Service slug is required."
            );
        }

        if (normalized.length() > 180) {
            throw badRequest(
                    "Service slug must not exceed 180 characters."
            );
        }

        return normalized;
    }

    private String normalizeCurrencyCode(
            String value
    ) {
        String normalized =
                normalizeOptional(value);

        if (normalized == null) {
            normalized = "USD";
        }

        normalized = normalized.toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw badRequest(
                    "Currency code must contain exactly three letters."
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

    private WebsiteServiceVersion saveVersion(
            WebsiteServiceVersion version,
            String conflictMessage
    ) {
        try {
            return websiteServiceVersionRepository
                    .saveAndFlush(version);
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

    private void validateLength(
            String value,
            int maximumLength,
            String fieldName
    ) {
        validateLength(
                value,
                maximumLength,
                fieldName,
                false
        );
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