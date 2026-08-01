package romelt_techcare.backend.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.entity.WebsiteMediaUsage;
import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;
import romelt_techcare.backend.enums.WebsiteMediaUsageResourceType;
import romelt_techcare.backend.repository.WebsiteMediaAssetRepository;
import romelt_techcare.backend.repository.WebsiteMediaUsageRepository;
import romelt_techcare.backend.service.WebsiteMediaUsageService;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA USAGE SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements system-managed media-reference tracking for the Romelt
 * TechCare website CMS.
 *
 * Responsibilities:
 * - Registers exact media-to-resource references.
 * - Preserves the uniqueness rule defined by the database schema.
 * - Replaces single-value resource-field references transactionally.
 * - Supports multi-value resource fields through registerUsage.
 * - Retrieves usage records for administrator inspection.
 * - Removes obsolete references.
 * - Blocks deletion while references remain.
 *
 * Schema alignment:
 * The database unique constraint is:
 *
 * media_asset_id + resource_type + resource_id + usage_field
 *
 * This permits multiple different media assets to reference the same
 * resource field. That is useful for galleries and image collections.
 *
 * replaceUsage treats a resource field as single-value and removes all
 * existing references for that field before registering the replacement.
 * registerUsage does not remove other references and is therefore
 * appropriate for multi-value fields.
 *
 * Resource ownership:
 * The owning CMS service must validate its resource before calling
 * this service because resourceId may refer to different tables.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebsiteMediaUsageServiceImplementation
        implements WebsiteMediaUsageService {

    private final WebsiteMediaUsageRepository websiteMediaUsageRepository;
    private final WebsiteMediaAssetRepository websiteMediaAssetRepository;

    /**
     * Registers an exact usage or returns the already existing record.
     */
    @Override
    @Transactional
    public WebsiteMediaUsage registerUsage(
            UUID mediaAssetId,
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId,
            String usageField,
            String usageDescription
    ) {
        requireIdentifier(mediaAssetId, "Media asset ID");
        requireResourceType(resourceType);
        requireIdentifier(resourceId, "Media usage resource ID");

        String normalizedUsageField =
                normalizeUsageField(usageField);

        String normalizedDescription =
                normalizeDescription(usageDescription);

        WebsiteMediaAsset mediaAsset =
                getUsableMediaAsset(mediaAssetId);

        WebsiteMediaUsage existingUsage =
                websiteMediaUsageRepository
                        .findByMediaAsset_MediaAssetIdAndResourceTypeAndResourceIdAndUsageField(
                                mediaAssetId,
                                resourceType,
                                resourceId,
                                normalizedUsageField
                        )
                        .orElse(null);

        if (existingUsage != null) {
            if (!Objects.equals(
                    existingUsage.getUsageDescription(),
                    normalizedDescription
            )) {
                existingUsage.setUsageDescription(
                        normalizedDescription
                );

                return saveUsage(existingUsage);
            }

            return existingUsage;
        }

        WebsiteMediaUsage usage =
                WebsiteMediaUsage.builder()
                        .mediaAsset(mediaAsset)
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .usageField(normalizedUsageField)
                        .usageDescription(normalizedDescription)
                        .build();

        return saveUsage(usage);
    }

    /**
     * Replaces all current media references assigned to one
     * single-value resource field.
     */
    @Override
    @Transactional
    public WebsiteMediaUsage replaceUsage(
            UUID newMediaAssetId,
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId,
            String usageField,
            String usageDescription
    ) {
        requireResourceType(resourceType);
        requireIdentifier(resourceId, "Media usage resource ID");

        String normalizedUsageField =
                normalizeUsageField(usageField);

        if (newMediaAssetId == null) {
            websiteMediaUsageRepository
                    .deleteAllByResourceTypeAndResourceIdAndUsageField(
                            resourceType,
                            resourceId,
                            normalizedUsageField
                    );

            return null;
        }

        WebsiteMediaAsset newMediaAsset =
                getUsableMediaAsset(newMediaAssetId);

        List<WebsiteMediaUsage> currentUsages =
                websiteMediaUsageRepository
                        .findAllByResourceTypeAndResourceIdAndUsageFieldOrderByCreatedAtAsc(
                                resourceType,
                                resourceId,
                                normalizedUsageField
                        );

        if (currentUsages.size() == 1) {
            WebsiteMediaUsage currentUsage =
                    currentUsages.getFirst();

            UUID currentMediaAssetId =
                    currentUsage.getMediaAsset() == null
                            ? null
                            : currentUsage
                            .getMediaAsset()
                            .getMediaAssetId();

            if (newMediaAssetId.equals(currentMediaAssetId)) {
                currentUsage.setUsageDescription(
                        normalizeDescription(usageDescription)
                );

                return saveUsage(currentUsage);
            }
        }

        if (!currentUsages.isEmpty()) {
            websiteMediaUsageRepository
                    .deleteAllByResourceTypeAndResourceIdAndUsageField(
                            resourceType,
                            resourceId,
                            normalizedUsageField
                    );

            websiteMediaUsageRepository.flush();
        }

        WebsiteMediaUsage replacement =
                WebsiteMediaUsage.builder()
                        .mediaAsset(newMediaAsset)
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .usageField(normalizedUsageField)
                        .usageDescription(
                                normalizeDescription(usageDescription)
                        )
                        .build();

        return saveUsage(replacement);
    }

    /**
     * Retrieves one usage and its media asset.
     */
    @Override
    public WebsiteMediaUsage getUsage(
            UUID mediaUsageId
    ) {
        requireIdentifier(mediaUsageId, "Media usage ID");

        return websiteMediaUsageRepository
                .findByMediaUsageId(mediaUsageId)
                .orElseThrow(() -> notFound(
                        "Website media usage was not found."
                ));
    }

    /**
     * Retrieves paginated usage records for one media asset.
     */
    @Override
    public Page<WebsiteMediaUsage> getUsagesByMediaAsset(
            UUID mediaAssetId,
            Pageable pageable
    ) {
        requireIdentifier(mediaAssetId, "Media asset ID");
        requirePageable(pageable);

        if (!websiteMediaAssetRepository.existsById(mediaAssetId)) {
            throw notFound(
                    "Website media asset was not found."
            );
        }

        return websiteMediaUsageRepository
                .findAllByMediaAsset_MediaAssetIdOrderByCreatedAtDesc(
                        mediaAssetId,
                        pageable
                );
    }

    /**
     * Retrieves every media reference belonging to one resource.
     */
    @Override
    public List<WebsiteMediaUsage> getUsagesByResource(
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId
    ) {
        requireResourceType(resourceType);
        requireIdentifier(resourceId, "Media usage resource ID");

        return websiteMediaUsageRepository
                .findAllByResourceTypeAndResourceIdOrderByCreatedAtAsc(
                        resourceType,
                        resourceId
                );
    }

    /**
     * Removes one exact usage record.
     */
    @Override
    @Transactional
    public void removeUsage(
            UUID mediaUsageId
    ) {
        WebsiteMediaUsage usage = getUsage(mediaUsageId);

        websiteMediaUsageRepository.delete(usage);
    }

    /**
     * Removes every media reference assigned to one resource field.
     */
    @Override
    @Transactional
    public long removeUsagesByResourceField(
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId,
            String usageField
    ) {
        requireResourceType(resourceType);
        requireIdentifier(resourceId, "Media usage resource ID");

        String normalizedUsageField =
                normalizeUsageField(usageField);

        return websiteMediaUsageRepository
                .deleteAllByResourceTypeAndResourceIdAndUsageField(
                        resourceType,
                        resourceId,
                        normalizedUsageField
                );
    }

    /**
     * Removes every media reference belonging to one resource.
     */
    @Override
    @Transactional
    public long removeAllUsagesForResource(
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId
    ) {
        requireResourceType(resourceType);
        requireIdentifier(resourceId, "Media usage resource ID");

        return websiteMediaUsageRepository
                .deleteAllByResourceTypeAndResourceId(
                        resourceType,
                        resourceId
                );
    }

    /**
     * Determines whether a media asset has references.
     */
    @Override
    public boolean isMediaAssetInUse(
            UUID mediaAssetId
    ) {
        requireIdentifier(mediaAssetId, "Media asset ID");

        return websiteMediaUsageRepository
                .existsByMediaAsset_MediaAssetId(mediaAssetId);
    }

    /**
     * Counts references to a media asset.
     */
    @Override
    public long countUsages(
            UUID mediaAssetId
    ) {
        requireIdentifier(mediaAssetId, "Media asset ID");

        return websiteMediaUsageRepository
                .countByMediaAsset_MediaAssetId(mediaAssetId);
    }

    /**
     * Prevents deletion while references remain.
     */
    @Override
    public void requireMediaAssetNotInUse(
            UUID mediaAssetId
    ) {
        requireIdentifier(mediaAssetId, "Media asset ID");

        long usageCount =
                websiteMediaUsageRepository
                        .countByMediaAsset_MediaAssetId(
                                mediaAssetId
                        );

        if (usageCount > 0) {
            throw conflict(
                    "The media asset cannot be deleted because it is "
                            + "currently used by "
                            + usageCount
                            + (usageCount == 1
                            ? " website resource."
                            : " website resources.")
            );
        }
    }

    /**
     * Retrieves a media asset that may be assigned to CMS content.
     */
    private WebsiteMediaAsset getUsableMediaAsset(
            UUID mediaAssetId
    ) {
        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetRepository
                        .findByMediaAssetIdAndDeletedAtIsNull(
                                mediaAssetId
                        )
                        .orElseThrow(() -> notFound(
                                "Website media asset was not found."
                        ));

        WebsiteMediaAssetStatus status =
                mediaAsset.getAssetStatus();

        if (status == WebsiteMediaAssetStatus.DELETED) {
            throw conflict(
                    "A deleted media asset cannot be assigned to content."
            );
        }

        if (status == WebsiteMediaAssetStatus.FAILED) {
            throw conflict(
                    "A failed media asset cannot be assigned to content."
            );
        }

        if (status == WebsiteMediaAssetStatus.UPLOADING) {
            throw conflict(
                    "The media asset cannot be assigned while its upload "
                            + "is incomplete."
            );
        }

        if (status == WebsiteMediaAssetStatus.PROCESSING) {
            throw conflict(
                    "The media asset cannot be assigned while processing "
                            + "is incomplete."
            );
        }

        return mediaAsset;
    }

    /**
     * Persists a usage and converts uniqueness violations into a
     * conflict response.
     */
    private WebsiteMediaUsage saveUsage(
            WebsiteMediaUsage usage
    ) {
        try {
            return websiteMediaUsageRepository.saveAndFlush(
                    usage
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The website media usage is already registered.",
                    exception
            );
        }
    }

    /**
     * Normalizes a usage-field key.
     */
    private String normalizeUsageField(
            String usageField
    ) {
        String normalized =
                normalizeOptional(usageField);

        if (normalized == null) {
            throw badRequest(
                    "Media usage field is required."
            );
        }

        normalized = normalized
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        if (normalized.isBlank()) {
            throw badRequest(
                    "Media usage field is required."
            );
        }

        if (normalized.length() > 120) {
            throw badRequest(
                    "Media usage field must not exceed 120 characters."
            );
        }

        return normalized;
    }

    /**
     * Normalizes and validates a usage description.
     */
    private String normalizeDescription(
            String description
    ) {
        String normalized =
                normalizeOptional(description);

        if (normalized != null && normalized.length() > 500) {
            throw badRequest(
                    "Media usage description must not exceed 500 characters."
            );
        }

        return normalized;
    }

    /**
     * Requires pagination information.
     */
    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw badRequest(
                    "Pagination information is required."
            );
        }
    }

    /**
     * Requires a resource type.
     */
    private void requireResourceType(
            WebsiteMediaUsageResourceType resourceType
    ) {
        if (resourceType == null) {
            throw badRequest(
                    "Media usage resource type is required."
            );
        }
    }

    /**
     * Requires a UUID identifier.
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
     * Trims an optional value and converts blanks to null.
     */
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