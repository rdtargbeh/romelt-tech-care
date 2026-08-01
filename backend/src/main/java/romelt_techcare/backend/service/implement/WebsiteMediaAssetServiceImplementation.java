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
import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.WebsiteMediaAssetRepository;
import romelt_techcare.backend.service.WebsiteMediaAssetService;
import romelt_techcare.backend.service.WebsiteMediaUsageService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA ASSET SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements the business rules for website-facing media assets used
 * by the Romelt TechCare content-management system.
 *
 * Responsibilities:
 * - Creates and updates media metadata.
 * - Enforces asset-key and file-attachment uniqueness.
 * - Records the administrator responsible for each operation.
 * - Protects lifecycle and optimistic-lock fields from direct updates.
 * - Manages upload, processing, failure, active, archived, and deleted
 *   lifecycle states.
 * - Enforces accessibility requirements for public media.
 * - Supports administrative search and public media retrieval.
 * - Performs media lifecycle updates inside database transactions.
 *
 * Binary file responsibility:
 * This implementation does not upload, download, scan, resize,
 * optimize, or physically delete binary files. Those operations belong
 * to the shared file-storage service.
 *
 * Media-usage responsibility:
 * Before permanent deletion is introduced, the future
 * WebsiteMediaUsage service must confirm that the asset has no active
 * references. This implementation performs soft deletion only.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteMediaAssetServiceImplementation
        implements WebsiteMediaAssetService {

    private final WebsiteMediaAssetRepository websiteMediaAssetRepository;
    private final WebsiteMediaUsageService websiteMediaUsageService;
    private final AdminUserRepository adminUserRepository;

    /**
     * Creates a new website media asset.
     */
    @Override
    @Transactional
    public WebsiteMediaAsset createMediaAsset(
            WebsiteMediaAsset mediaAsset,
            UUID administratorId
    ) {
        if (mediaAsset == null) {
            throw badRequest(
                    "Media asset information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        String normalizedAssetKey =
                normalizeAssetKey(mediaAsset.getAssetKey());

        validateAssetKeyAvailability(
                normalizedAssetKey,
                null
        );

        validateFileAttachmentAvailability(
                mediaAsset.getFileAttachmentId(),
                null
        );

        validateRequiredMediaFields(mediaAsset);
        validateNumericMetadata(mediaAsset);

        mediaAsset.setMediaAssetId(null);
        mediaAsset.setAssetKey(normalizedAssetKey);

        mediaAsset.setCreatedByAdminUser(administrator);
        mediaAsset.setUpdatedByAdminUser(administrator);

        /*
         * Lifecycle and audit values cannot be supplied by an external
         * caller during creation.
         */
        mediaAsset.setArchivedByAdminUser(null);
        mediaAsset.setDeletedByAdminUser(null);
        mediaAsset.setArchivedAt(null);
        mediaAsset.setDeletedAt(null);
        mediaAsset.setRowVersion(null);

        if (mediaAsset.getAssetStatus() == null) {
            mediaAsset.setAssetStatus(
                    WebsiteMediaAssetStatus.ACTIVE
            );
        }

        if (mediaAsset.getIsPublic() == null) {
            mediaAsset.setIsPublic(true);
        }

        if (mediaAsset.getIsDecorative() == null) {
            mediaAsset.setIsDecorative(false);
        }

        validateLifecycleForCreation(mediaAsset);
        validatePublicAccessibility(mediaAsset);

        return saveMediaAsset(
                mediaAsset,
                "Unable to create the media asset because one of its "
                        + "unique values is already in use."
        );
    }

    /**
     * Updates editable media metadata while preserving lifecycle and
     * audit information.
     */
    @Override
    @Transactional
    public WebsiteMediaAsset updateMediaAsset(
            UUID mediaAssetId,
            WebsiteMediaAsset requestedUpdate,
            UUID administratorId
    ) {
        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated media asset information is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteMediaAsset existingMediaAsset =
                getMediaAssetForUpdate(mediaAssetId);

        ensureEditable(existingMediaAsset);

        String normalizedAssetKey =
                normalizeAssetKey(requestedUpdate.getAssetKey());

        validateAssetKeyAvailability(
                normalizedAssetKey,
                existingMediaAsset.getMediaAssetId()
        );

        validateFileAttachmentAvailability(
                requestedUpdate.getFileAttachmentId(),
                existingMediaAsset.getMediaAssetId()
        );

        /*
         * General editable metadata.
         */
        existingMediaAsset.setFileAttachmentId(
                requestedUpdate.getFileAttachmentId()
        );

        existingMediaAsset.setAssetKey(
                normalizedAssetKey
        );

        existingMediaAsset.setOriginalFileName(
                requestedUpdate.getOriginalFileName()
        );

        existingMediaAsset.setPublicUrl(
                requestedUpdate.getPublicUrl()
        );

        existingMediaAsset.setStorageKey(
                requestedUpdate.getStorageKey()
        );

        existingMediaAsset.setMimeType(
                requestedUpdate.getMimeType()
        );

        existingMediaAsset.setFileExtension(
                requestedUpdate.getFileExtension()
        );

        existingMediaAsset.setFileSizeBytes(
                requestedUpdate.getFileSizeBytes()
        );

        existingMediaAsset.setWidthPixels(
                requestedUpdate.getWidthPixels()
        );

        existingMediaAsset.setHeightPixels(
                requestedUpdate.getHeightPixels()
        );

        existingMediaAsset.setTitle(
                requestedUpdate.getTitle()
        );

        existingMediaAsset.setAltText(
                requestedUpdate.getAltText()
        );

        existingMediaAsset.setCaption(
                requestedUpdate.getCaption()
        );

        existingMediaAsset.setDescription(
                requestedUpdate.getDescription()
        );

        existingMediaAsset.setIsDecorative(
                requestedUpdate.getIsDecorative() != null
                        ? requestedUpdate.getIsDecorative()
                        : false
        );

        existingMediaAsset.setFocalPointX(
                requestedUpdate.getFocalPointX()
        );

        existingMediaAsset.setFocalPointY(
                requestedUpdate.getFocalPointY()
        );

        /*
         * Public visibility can be changed through normal editing, but
         * lifecycle status cannot be overwritten through this method.
         */
        if (requestedUpdate.getIsPublic() != null) {
            existingMediaAsset.setIsPublic(
                    requestedUpdate.getIsPublic()
            );
        }

        existingMediaAsset.setUpdatedByAdminUser(
                administrator
        );

        validateRequiredMediaFields(existingMediaAsset);
        validateNumericMetadata(existingMediaAsset);
        validatePublicAccessibility(existingMediaAsset);

        return saveMediaAsset(
                existingMediaAsset,
                "Unable to update the media asset because one of its "
                        + "unique values is already in use."
        );
    }

    /**
     * Retrieves a non-deleted media asset.
     */
    @Override
    public WebsiteMediaAsset getMediaAsset(
            UUID mediaAssetId
    ) {
        requireIdentifier(
                mediaAssetId,
                "Media asset ID"
        );

        return websiteMediaAssetRepository
                .findByMediaAssetIdAndDeletedAtIsNull(mediaAssetId)
                .orElseThrow(() -> notFound(
                        "Website media asset was not found."
                ));
    }

    /**
     * Retrieves a non-deleted asset by stable key.
     */
    @Override
    public WebsiteMediaAsset getMediaAssetByAssetKey(
            String assetKey
    ) {
        String normalizedAssetKey =
                requireNormalizedAssetKey(assetKey);

        return websiteMediaAssetRepository
                .findByAssetKeyAndDeletedAtIsNull(normalizedAssetKey)
                .orElseThrow(() -> notFound(
                        "Website media asset was not found."
                ));
    }

    /**
     * Retrieves an active public asset by stable key.
     */
    @Override
    public WebsiteMediaAsset getPublicMediaAssetByAssetKey(
            String assetKey
    ) {
        String normalizedAssetKey =
                requireNormalizedAssetKey(assetKey);

        return websiteMediaAssetRepository
                .findByAssetKeyAndAssetStatusAndIsPublicTrueAndDeletedAtIsNull(
                        normalizedAssetKey,
                        WebsiteMediaAssetStatus.ACTIVE
                )
                .orElseThrow(() -> notFound(
                        "Public website media asset was not found."
                ));
    }

    /**
     * Retrieves an asset by shared file-attachment identifier.
     */
    @Override
    public WebsiteMediaAsset getMediaAssetByFileAttachmentId(
            UUID fileAttachmentId
    ) {
        requireIdentifier(
                fileAttachmentId,
                "File attachment ID"
        );

        return websiteMediaAssetRepository
                .findByFileAttachmentIdAndDeletedAtIsNull(
                        fileAttachmentId
                )
                .orElseThrow(() -> notFound(
                        "No website media asset is connected to the "
                                + "specified file attachment."
                ));
    }

    /**
     * Searches non-deleted media records.
     */
    @Override
    public Page<WebsiteMediaAsset> searchMediaAssets(
            String keyword,
            WebsiteMediaAssetStatus assetStatus,
            Boolean isPublic,
            String mimeType,
            Pageable pageable
    ) {
        if (pageable == null) {
            throw badRequest(
                    "Pagination information is required."
            );
        }

        return websiteMediaAssetRepository.searchMediaAssets(
                normalizeOptional(keyword),
                assetStatus,
                isPublic,
                normalizeMimeType(mimeType),
                pageable
        );
    }

    /**
     * Retrieves soft-deleted media records.
     */
    @Override
    public Page<WebsiteMediaAsset> getDeletedMediaAssets(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw badRequest(
                    "Pagination information is required."
            );
        }

        return websiteMediaAssetRepository
                .findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(
                        pageable
                );
    }

    /**
     * Marks a media asset as uploading.
     */
    @Override
    @Transactional
    public WebsiteMediaAsset markUploading(
            UUID mediaAssetId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteMediaAsset mediaAsset =
                getMediaAssetForUpdate(mediaAssetId);

        ensureNotDeleted(mediaAsset);

        mediaAsset.setAssetStatus(
                WebsiteMediaAssetStatus.UPLOADING
        );

        mediaAsset.setIsPublic(false);
        mediaAsset.setArchivedAt(null);
        mediaAsset.setArchivedByAdminUser(null);
        mediaAsset.setUpdatedByAdminUser(administrator);

        return websiteMediaAssetRepository.save(mediaAsset);
    }

    /**
     * Marks a media asset as being processed.
     */
    @Override
    @Transactional
    public WebsiteMediaAsset markProcessing(
            UUID mediaAssetId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteMediaAsset mediaAsset =
                getMediaAssetForUpdate(mediaAssetId);

        ensureNotDeleted(mediaAsset);

        mediaAsset.markProcessing(administrator);

        return websiteMediaAssetRepository.save(mediaAsset);
    }

    /**
     * Marks media processing as failed.
     */
    @Override
    @Transactional
    public WebsiteMediaAsset markFailed(
            UUID mediaAssetId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteMediaAsset mediaAsset =
                getMediaAssetForUpdate(mediaAssetId);

        ensureNotDeleted(mediaAsset);

        mediaAsset.markFailed(administrator);

        return websiteMediaAssetRepository.save(mediaAsset);
    }

    /**
     * Activates a successfully uploaded and validated media asset.
     */
    @Override
    @Transactional
    public WebsiteMediaAsset activateMediaAsset(
            UUID mediaAssetId,
            boolean makePublic,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteMediaAsset mediaAsset =
                getMediaAssetForUpdate(mediaAssetId);

        ensureNotDeleted(mediaAsset);

        validateRequiredMediaFields(mediaAsset);
        validateNumericMetadata(mediaAsset);

        mediaAsset.activate(administrator);
        mediaAsset.setIsPublic(makePublic);

        validatePublicAccessibility(mediaAsset);

        return websiteMediaAssetRepository.save(mediaAsset);
    }

    /**
     * Archives an active or inactive media asset.
     */
    @Override
    @Transactional
    public WebsiteMediaAsset archiveMediaAsset(
            UUID mediaAssetId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteMediaAsset mediaAsset =
                getMediaAssetForUpdate(mediaAssetId);

        ensureNotDeleted(mediaAsset);

        if (
                mediaAsset.getAssetStatus()
                        == WebsiteMediaAssetStatus.ARCHIVED
        ) {
            return mediaAsset;
        }

        mediaAsset.archive(administrator);

        return websiteMediaAssetRepository.save(mediaAsset);
    }

    /**
     * Restores an archived asset to an active private state.
     */
    @Override
    @Transactional
    public WebsiteMediaAsset restoreMediaAsset(
            UUID mediaAssetId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteMediaAsset mediaAsset =
                getMediaAssetForUpdate(mediaAssetId);

        ensureNotDeleted(mediaAsset);

        if (
                mediaAsset.getAssetStatus()
                        != WebsiteMediaAssetStatus.ARCHIVED
        ) {
            throw conflict(
                    "Only an archived media asset can be restored."
            );
        }

        mediaAsset.restore(administrator);

        /*
         * Restoration should not automatically expose an asset publicly.
         * An administrator must explicitly make it public afterward.
         */
        mediaAsset.setIsPublic(false);

        return websiteMediaAssetRepository.save(mediaAsset);
    }

    /**
     * Soft-deletes a media asset after verifying that no CMS or website
     * resource still references it.
     */
    @Override
    @Transactional
    public void deleteMediaAsset(
            UUID mediaAssetId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteMediaAsset mediaAsset =
                getMediaAssetForUpdate(mediaAssetId);

        ensureNotDeleted(mediaAsset);

        /*
         * Media deletion is blocked while the asset remains referenced by
         * a business profile, page version, service version, pricing plan,
         * customer review, navigation record, or another CMS resource.
         */
        websiteMediaUsageService.requireMediaAssetNotInUse(
                mediaAssetId
        );

        mediaAsset.softDelete(administrator);

        websiteMediaAssetRepository.save(mediaAsset);
    }

    /**
     * Updates public visibility.
     */
    @Override
    @Transactional
    public WebsiteMediaAsset updatePublicVisibility(
            UUID mediaAssetId,
            boolean isPublic,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteMediaAsset mediaAsset =
                getMediaAssetForUpdate(mediaAssetId);

        ensureNotDeleted(mediaAsset);

        if (
                isPublic
                        && mediaAsset.getAssetStatus()
                        != WebsiteMediaAssetStatus.ACTIVE
        ) {
            throw conflict(
                    "Only an active media asset can be made public."
            );
        }

        mediaAsset.setIsPublic(isPublic);
        mediaAsset.setUpdatedByAdminUser(administrator);

        validatePublicAccessibility(mediaAsset);

        return websiteMediaAssetRepository.save(mediaAsset);
    }

    /**
     * Updates accessible alternative-text information.
     */
    @Override
    @Transactional
    public WebsiteMediaAsset updateAccessibility(
            UUID mediaAssetId,
            String altText,
            boolean isDecorative,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteMediaAsset mediaAsset =
                getMediaAssetForUpdate(mediaAssetId);

        ensureNotDeleted(mediaAsset);

        mediaAsset.updateAccessibility(
                altText,
                isDecorative,
                administrator
        );

        validatePublicAccessibility(mediaAsset);

        return websiteMediaAssetRepository.save(mediaAsset);
    }

    /**
     * Updates image focal-point positioning.
     */
    @Override
    @Transactional
    public WebsiteMediaAsset updateFocalPoint(
            UUID mediaAssetId,
            BigDecimal focalPointX,
            BigDecimal focalPointY,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(administratorId);

        WebsiteMediaAsset mediaAsset =
                getMediaAssetForUpdate(mediaAssetId);

        ensureNotDeleted(mediaAsset);

        validateFocalPoint(
                focalPointX,
                "Horizontal focal point"
        );

        validateFocalPoint(
                focalPointY,
                "Vertical focal point"
        );

        mediaAsset.updateFocalPoint(
                focalPointX,
                focalPointY,
                administrator
        );

        return websiteMediaAssetRepository.save(mediaAsset);
    }

    /**
     * Counts active public media records.
     */
    @Override
    public long countActivePublicMediaAssets() {
        return websiteMediaAssetRepository
                .countByAssetStatusAndIsPublicTrueAndDeletedAtIsNull(
                        WebsiteMediaAssetStatus.ACTIVE
                );
    }

    /**
     * Counts soft-deleted media records.
     */
    @Override
    public long countDeletedMediaAssets() {
        return websiteMediaAssetRepository
                .countByDeletedAtIsNotNull();
    }

    /**
     * Retrieves and locks a non-deleted media asset for lifecycle updates.
     */
    private WebsiteMediaAsset getMediaAssetForUpdate(
            UUID mediaAssetId
    ) {
        requireIdentifier(
                mediaAssetId,
                "Media asset ID"
        );

        return websiteMediaAssetRepository
                .findByIdForUpdate(mediaAssetId)
                .orElseThrow(() -> notFound(
                        "Website media asset was not found."
                ));
    }

    /**
     * Retrieves the administrator responsible for an operation.
     */
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

    /**
     * Ensures a stable asset key is not assigned to another record.
     */
    private void validateAssetKeyAvailability(
            String normalizedAssetKey,
            UUID currentMediaAssetId
    ) {
        if (normalizedAssetKey == null) {
            return;
        }

        boolean exists;

        if (currentMediaAssetId == null) {
            exists = websiteMediaAssetRepository
                    .existsByAssetKey(normalizedAssetKey);
        } else {
            exists = websiteMediaAssetRepository
                    .existsByAssetKeyAndMediaAssetIdNot(
                            normalizedAssetKey,
                            currentMediaAssetId
                    );
        }

        if (exists) {
            throw conflict(
                    "The media asset key is already in use."
            );
        }
    }

    /**
     * Ensures a file attachment is not assigned to another media asset.
     */
    private void validateFileAttachmentAvailability(
            UUID fileAttachmentId,
            UUID currentMediaAssetId
    ) {
        if (fileAttachmentId == null) {
            return;
        }

        websiteMediaAssetRepository
                .findByFileAttachmentIdAndDeletedAtIsNull(
                        fileAttachmentId
                )
                .ifPresent(existingMediaAsset -> {
                    if (
                            currentMediaAssetId == null
                                    || !Objects.equals(
                                    existingMediaAsset.getMediaAssetId(),
                                    currentMediaAssetId
                            )
                    ) {
                        throw conflict(
                                "The file attachment is already registered "
                                        + "as a website media asset."
                        );
                    }
                });

        /*
         * The database unique constraint also covers deleted records.
         * This additional check handles cases where a deleted record still
         * owns the attachment identifier.
         */
        if (
                currentMediaAssetId == null
                        && websiteMediaAssetRepository
                        .existsByFileAttachmentId(fileAttachmentId)
        ) {
            throw conflict(
                    "The file attachment is already registered as a "
                            + "website media asset."
            );
        }
    }

    /**
     * Validates fields required for persistence.
     */
    private void validateRequiredMediaFields(
            WebsiteMediaAsset mediaAsset
    ) {
        if (isBlank(mediaAsset.getOriginalFileName())) {
            throw badRequest(
                    "Original file name is required."
            );
        }

        if (isBlank(mediaAsset.getMimeType())) {
            throw badRequest(
                    "MIME type is required."
            );
        }
    }

    /**
     * Validates numeric media information.
     */
    private void validateNumericMetadata(
            WebsiteMediaAsset mediaAsset
    ) {
        if (
                mediaAsset.getFileSizeBytes() != null
                        && mediaAsset.getFileSizeBytes() < 0
        ) {
            throw badRequest(
                    "File size must not be negative."
            );
        }

        if (
                mediaAsset.getWidthPixels() != null
                        && mediaAsset.getWidthPixels() <= 0
        ) {
            throw badRequest(
                    "Image width must be greater than zero."
            );
        }

        if (
                mediaAsset.getHeightPixels() != null
                        && mediaAsset.getHeightPixels() <= 0
        ) {
            throw badRequest(
                    "Image height must be greater than zero."
            );
        }

        validateFocalPoint(
                mediaAsset.getFocalPointX(),
                "Horizontal focal point"
        );

        validateFocalPoint(
                mediaAsset.getFocalPointY(),
                "Vertical focal point"
        );
    }

    /**
     * Validates the initial lifecycle state.
     */
    private void validateLifecycleForCreation(
            WebsiteMediaAsset mediaAsset
    ) {
        if (
                mediaAsset.getAssetStatus()
                        == WebsiteMediaAssetStatus.ARCHIVED
        ) {
            throw badRequest(
                    "A new media asset cannot initially be archived."
            );
        }

        if (
                mediaAsset.getAssetStatus()
                        == WebsiteMediaAssetStatus.DELETED
        ) {
            throw badRequest(
                    "A new media asset cannot initially be deleted."
            );
        }

        if (
                mediaAsset.getAssetStatus()
                        != WebsiteMediaAssetStatus.ACTIVE
        ) {
            mediaAsset.setIsPublic(false);
        }
    }

    /**
     * Validates public accessibility requirements.
     */
    private void validatePublicAccessibility(
            WebsiteMediaAsset mediaAsset
    ) {
        if (
                Boolean.TRUE.equals(mediaAsset.getIsPublic())
                        && !Boolean.TRUE.equals(
                        mediaAsset.getIsDecorative()
                )
                        && isBlank(mediaAsset.getAltText())
        ) {
            throw badRequest(
                    "A public media asset must include alternative text "
                            + "unless it is marked as decorative."
            );
        }
    }

    /**
     * Ensures normal updates cannot modify deleted media.
     */
    private void ensureNotDeleted(
            WebsiteMediaAsset mediaAsset
    ) {
        if (
                mediaAsset.getDeletedAt() != null
                        || mediaAsset.getAssetStatus()
                        == WebsiteMediaAssetStatus.DELETED
        ) {
            throw conflict(
                    "The media asset has been deleted and cannot be modified."
            );
        }
    }

    /**
     * Ensures general metadata edits are allowed.
     */
    private void ensureEditable(
            WebsiteMediaAsset mediaAsset
    ) {
        ensureNotDeleted(mediaAsset);

        if (
                mediaAsset.getAssetStatus()
                        == WebsiteMediaAssetStatus.ARCHIVED
        ) {
            throw conflict(
                    "Restore the archived media asset before editing it."
            );
        }
    }

    /**
     * Persists a media asset and converts unique-constraint errors into
     * a clear HTTP conflict response.
     */
    private WebsiteMediaAsset saveMediaAsset(
            WebsiteMediaAsset mediaAsset,
            String conflictMessage
    ) {
        try {
            return websiteMediaAssetRepository.saveAndFlush(
                    mediaAsset
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    conflictMessage,
                    exception
            );
        }
    }

    /**
     * Validates a focal-point percentage.
     */
    private void validateFocalPoint(
            BigDecimal value,
            String fieldName
    ) {
        if (value == null) {
            return;
        }

        if (
                value.compareTo(BigDecimal.ZERO) < 0
                        || value.compareTo(
                        BigDecimal.valueOf(100)
                ) > 0
        ) {
            throw badRequest(
                    fieldName + " must be between 0 and 100."
            );
        }
    }

    /**
     * Normalizes an optional asset key.
     */
    private String normalizeAssetKey(
            String assetKey
    ) {
        String normalizedValue =
                normalizeOptional(assetKey);

        if (normalizedValue == null) {
            return null;
        }

        normalizedValue = normalizedValue
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        return normalizedValue.isBlank()
                ? null
                : normalizedValue;
    }

    /**
     * Requires and normalizes an asset key.
     */
    private String requireNormalizedAssetKey(
            String assetKey
    ) {
        String normalizedAssetKey =
                normalizeAssetKey(assetKey);

        if (normalizedAssetKey == null) {
            throw badRequest(
                    "Media asset key is required."
            );
        }

        return normalizedAssetKey;
    }

    /**
     * Normalizes an optional MIME type.
     */
    private String normalizeMimeType(
            String mimeType
    ) {
        String normalizedValue =
                normalizeOptional(mimeType);

        return normalizedValue == null
                ? null
                : normalizedValue.toLowerCase(Locale.ROOT);
    }

    /**
     * Trims an optional string and converts blanks to null.
     */
    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }

    /**
     * Validates a required UUID identifier.
     */
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

    /**
     * Returns true when a string is null or blank.
     */
    private boolean isBlank(
            String value
    ) {
        return value == null
                || value.trim().isEmpty();
    }

    /**
     * Creates a 400 Bad Request response.
     */
    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    /**
     * Creates a 404 Not Found response.
     */
    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }

    /**
     * Creates a 409 Conflict response.
     */
    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}