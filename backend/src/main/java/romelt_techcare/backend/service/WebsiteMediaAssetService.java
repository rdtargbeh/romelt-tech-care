package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA ASSET SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines the business operations for website-facing media assets
 * managed through the Romelt TechCare content-management system.
 *
 * Responsibilities:
 * - Creates and updates media metadata.
 * - Retrieves administrator and public media records.
 * - Searches media using filters and pagination.
 * - Manages upload and processing lifecycle states.
 * - Archives, restores, activates, and soft-deletes media.
 * - Updates accessibility and image focal-point information.
 * - Enforces administrator attribution for media changes.
 * - Prevents duplicate asset keys and file-attachment mappings.
 *
 * Storage responsibility:
 * This service manages media metadata only. Uploading, downloading,
 * scanning, optimizing, and physically deleting binary files belong
 * to the shared file-storage service or object-storage integration.
 *
 * Deletion rule:
 * Media is soft-deleted. Permanent deletion must later verify that no
 * active WebsiteMediaUsage record references the media asset.
 * ================================================================
 */
public interface WebsiteMediaAssetService {

    /**
     * Creates a website media asset record.
     *
     * @param mediaAsset media metadata to create
     * @param administratorId administrator performing the operation
     * @return persisted media asset
     */
    WebsiteMediaAsset createMediaAsset(
            WebsiteMediaAsset mediaAsset,
            UUID administratorId
    );

    /**
     * Updates editable media metadata.
     *
     * Lifecycle and audit fields are protected from direct replacement.
     *
     * @param mediaAssetId media asset identifier
     * @param mediaAsset updated media metadata
     * @param administratorId administrator performing the operation
     * @return updated media asset
     */
    WebsiteMediaAsset updateMediaAsset(
            UUID mediaAssetId,
            WebsiteMediaAsset mediaAsset,
            UUID administratorId
    );

    /**
     * Retrieves a non-deleted media asset by identifier.
     *
     * @param mediaAssetId media asset identifier
     * @return matching media asset
     */
    WebsiteMediaAsset getMediaAsset(
            UUID mediaAssetId
    );

    /**
     * Retrieves a media asset by stable asset key.
     *
     * @param assetKey normalized or unnormalized asset key
     * @return matching non-deleted media asset
     */
    WebsiteMediaAsset getMediaAssetByAssetKey(
            String assetKey
    );

    /**
     * Retrieves an active public media asset by stable asset key.
     *
     * @param assetKey normalized or unnormalized asset key
     * @return active public media asset
     */
    WebsiteMediaAsset getPublicMediaAssetByAssetKey(
            String assetKey
    );

    /**
     * Retrieves the media asset connected to a shared file attachment.
     *
     * @param fileAttachmentId file attachment identifier
     * @return matching non-deleted media asset
     */
    WebsiteMediaAsset getMediaAssetByFileAttachmentId(
            UUID fileAttachmentId
    );

    /**
     * Searches non-deleted media assets using optional filters.
     *
     * @param keyword optional search text
     * @param assetStatus optional lifecycle status
     * @param isPublic optional public-visibility filter
     * @param mimeType optional exact MIME-type filter
     * @param pageable pagination and sorting information
     * @return matching media assets
     */
    Page<WebsiteMediaAsset> searchMediaAssets(
            String keyword,
            WebsiteMediaAssetStatus assetStatus,
            Boolean isPublic,
            String mimeType,
            Pageable pageable
    );

    /**
     * Retrieves soft-deleted media records.
     *
     * @param pageable pagination and sorting information
     * @return deleted media assets
     */
    Page<WebsiteMediaAsset> getDeletedMediaAssets(
            Pageable pageable
    );

    /**
     * Marks an asset as being uploaded.
     *
     * @param mediaAssetId media asset identifier
     * @param administratorId administrator performing the operation
     * @return updated media asset
     */
    WebsiteMediaAsset markUploading(
            UUID mediaAssetId,
            UUID administratorId
    );

    /**
     * Marks an asset as being processed.
     *
     * @param mediaAssetId media asset identifier
     * @param administratorId administrator performing the operation
     * @return updated media asset
     */
    WebsiteMediaAsset markProcessing(
            UUID mediaAssetId,
            UUID administratorId
    );

    /**
     * Marks upload or processing as failed.
     *
     * @param mediaAssetId media asset identifier
     * @param administratorId administrator performing the operation
     * @return updated media asset
     */
    WebsiteMediaAsset markFailed(
            UUID mediaAssetId,
            UUID administratorId
    );

    /**
     * Activates a successfully uploaded and validated media asset.
     *
     * @param mediaAssetId media asset identifier
     * @param makePublic whether the asset should be publicly available
     * @param administratorId administrator performing the operation
     * @return active media asset
     */
    WebsiteMediaAsset activateMediaAsset(
            UUID mediaAssetId,
            boolean makePublic,
            UUID administratorId
    );

    /**
     * Archives a media asset.
     *
     * Archived assets remain stored but are not publicly available.
     *
     * @param mediaAssetId media asset identifier
     * @param administratorId administrator performing the operation
     * @return archived media asset
     */
    WebsiteMediaAsset archiveMediaAsset(
            UUID mediaAssetId,
            UUID administratorId
    );

    /**
     * Restores an archived media asset to active status.
     *
     * The asset remains private until explicitly made public.
     *
     * @param mediaAssetId media asset identifier
     * @param administratorId administrator performing the operation
     * @return restored media asset
     */
    WebsiteMediaAsset restoreMediaAsset(
            UUID mediaAssetId,
            UUID administratorId
    );

    /**
     * Soft-deletes a media asset.
     *
     * The service does not physically delete the media record or file.
     *
     * @param mediaAssetId media asset identifier
     * @param administratorId administrator performing the operation
     */

    void deleteMediaAsset(
            UUID mediaAssetId,
            UUID administratorId
    );

    /**
     * Updates public visibility.
     *
     * Public accessibility requirements are validated before saving.
     *
     * @param mediaAssetId media asset identifier
     * @param isPublic new public visibility
     * @param administratorId administrator performing the operation
     * @return updated media asset
     */
    WebsiteMediaAsset updatePublicVisibility(
            UUID mediaAssetId,
            boolean isPublic,
            UUID administratorId
    );

    /**
     * Updates accessibility metadata.
     *
     * @param mediaAssetId media asset identifier
     * @param altText alternative text
     * @param isDecorative whether the asset is decorative
     * @param administratorId administrator performing the operation
     * @return updated media asset
     */
    WebsiteMediaAsset updateAccessibility(
            UUID mediaAssetId,
            String altText,
            boolean isDecorative,
            UUID administratorId
    );

    /**
     * Updates the image focal point.
     *
     * @param mediaAssetId media asset identifier
     * @param focalPointX horizontal percentage from 0 to 100
     * @param focalPointY vertical percentage from 0 to 100
     * @param administratorId administrator performing the operation
     * @return updated media asset
     */
    WebsiteMediaAsset updateFocalPoint(
            UUID mediaAssetId,
            BigDecimal focalPointX,
            BigDecimal focalPointY,
            UUID administratorId
    );

    /**
     * Counts active public media assets.
     *
     * @return active public media count
     */
    long countActivePublicMediaAssets();

    /**
     * Counts soft-deleted media assets.
     *
     * @return deleted media count
     */
    long countDeletedMediaAssets();
}