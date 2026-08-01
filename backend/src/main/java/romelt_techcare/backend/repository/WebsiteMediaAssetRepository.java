package romelt_techcare.backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.enums.WebsiteMediaAssetStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA ASSET REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides database access for media assets managed through the
 * Romelt TechCare website content-management system.
 *
 * Responsibilities:
 * - Retrieves active and public media assets.
 * - Finds assets by stable asset key.
 * - Finds assets connected to the shared file-storage module.
 * - Supports administrative filtering and pagination.
 * - Excludes soft-deleted records from normal lookups.
 * - Supports pessimistic locking for lifecycle operations.
 *
 * Real-data integration:
 * Used by:
 * - Website media administration services
 * - Public content delivery services
 * - Branding and page-content services
 * - Media usage and deletion validation
 * ================================================================
 */
@Repository
public interface WebsiteMediaAssetRepository
        extends JpaRepository<WebsiteMediaAsset, UUID> {

    /**
     * Finds a non-deleted media asset by identifier.
     */
    Optional<WebsiteMediaAsset>
    findByMediaAssetIdAndDeletedAtIsNull(
            UUID mediaAssetId
    );

    /**
     * Finds a media asset by its normalized stable key.
     */
    Optional<WebsiteMediaAsset>
    findByAssetKeyAndDeletedAtIsNull(
            String assetKey
    );

    /**
     * Finds an active public media asset by its normalized stable key.
     */
    Optional<WebsiteMediaAsset>
    findByAssetKeyAndAssetStatusAndIsPublicTrueAndDeletedAtIsNull(
            String assetKey,
            WebsiteMediaAssetStatus assetStatus
    );

    /**
     * Finds the CMS media asset associated with a shared file attachment.
     */
    Optional<WebsiteMediaAsset>
    findByFileAttachmentIdAndDeletedAtIsNull(
            UUID fileAttachmentId
    );

    /**
     * Checks whether a stable media key already exists.
     */
    boolean existsByAssetKey(
            String assetKey
    );

    /**
     * Checks whether another media asset already uses a stable key.
     */
    boolean existsByAssetKeyAndMediaAssetIdNot(
            String assetKey,
            UUID mediaAssetId
    );

    /**
     * Checks whether a shared file attachment is already registered as
     * a CMS media asset.
     */
    boolean existsByFileAttachmentId(
            UUID fileAttachmentId
    );

    /**
     * Returns active public media ordered by newest first.
     */
    Page<WebsiteMediaAsset>
    findAllByAssetStatusAndIsPublicTrueAndDeletedAtIsNullOrderByCreatedAtDesc(
            WebsiteMediaAssetStatus assetStatus,
            Pageable pageable
    );

    /**
     * Returns non-deleted media by status.
     */
    Page<WebsiteMediaAsset>
    findAllByAssetStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            WebsiteMediaAssetStatus assetStatus,
            Pageable pageable
    );

    /**
     * Returns non-deleted media by MIME type.
     */
    Page<WebsiteMediaAsset>
    findAllByMimeTypeIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDesc(
            String mimeType,
            Pageable pageable
    );

    /**
     * Returns all soft-deleted media records.
     */
    Page<WebsiteMediaAsset>
    findAllByDeletedAtIsNotNullOrderByDeletedAtDesc(
            Pageable pageable
    );

    /**
     * Returns active public assets for lightweight public delivery.
     */
    List<WebsiteMediaAsset>
    findAllByAssetStatusAndIsPublicTrueAndDeletedAtIsNullOrderByCreatedAtDesc(
            WebsiteMediaAssetStatus assetStatus
    );

    /**
     * Retrieves a media asset with a pessimistic write lock.
     *
     * Use for archive, restore, deletion, and other lifecycle operations
     * that must not be executed concurrently.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select media
            from WebsiteMediaAsset media
            where media.mediaAssetId = :mediaAssetId
              and media.deletedAt is null
            """)
    Optional<WebsiteMediaAsset>
    findByIdForUpdate(
            @Param("mediaAssetId")
            UUID mediaAssetId
    );

    /**
     * Searches non-deleted media by file name, title, asset key,
     * alternative text, MIME type, or caption.
     */
    @Query("""
            select media
            from WebsiteMediaAsset media
            where media.deletedAt is null
              and (
                    :keyword is null
                    or lower(media.originalFileName)
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(media.title, ''))
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(media.assetKey, ''))
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(media.altText, ''))
                        like lower(concat('%', :keyword, '%'))
                    or lower(media.mimeType)
                        like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(media.caption, ''))
                        like lower(concat('%', :keyword, '%'))
                  )
              and (
                    :assetStatus is null
                    or media.assetStatus = :assetStatus
                  )
              and (
                    :isPublic is null
                    or media.isPublic = :isPublic
                  )
              and (
                    :mimeType is null
                    or lower(media.mimeType) = lower(:mimeType)
                  )
            order by media.createdAt desc
            """)
    Page<WebsiteMediaAsset>
    searchMediaAssets(
            @Param("keyword")
            String keyword,

            @Param("assetStatus")
            WebsiteMediaAssetStatus assetStatus,

            @Param("isPublic")
            Boolean isPublic,

            @Param("mimeType")
            String mimeType,

            Pageable pageable
    );

    /**
     * Counts active public media records.
     */
    long countByAssetStatusAndIsPublicTrueAndDeletedAtIsNull(
            WebsiteMediaAssetStatus assetStatus
    );

    /**
     * Counts soft-deleted media records.
     */
    long countByDeletedAtIsNotNull();
}