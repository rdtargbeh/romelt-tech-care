package romelt_techcare.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import romelt_techcare.backend.entity.WebsiteMediaUsage;
import romelt_techcare.backend.enums.WebsiteMediaUsageResourceType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA USAGE REPOSITORY
 * ================================================================
 *
 * Purpose:
 * Provides database access for records that identify where website
 * media assets are currently used.
 *
 * Responsibilities:
 * - Retrieves references for one media asset.
 * - Retrieves media references belonging to one CMS resource.
 * - Detects exact duplicate references.
 * - Counts references before archive or deletion operations.
 * - Removes references when the owning CMS resource changes.
 * - Fetches the associated WebsiteMediaAsset when responses require it.
 *
 * Performance:
 * Entity graphs are used on response-facing retrieval methods so the
 * associated media asset is loaded in the same query. This prevents
 * lazy-loading failures and unnecessary follow-up queries while
 * mapping API responses.
 *
 * Ownership:
 * Usage records are system-managed. Page, service, pricing, branding,
 * navigation, and review services create or replace these references.
 * They are not ordinary administrator-created content records.
 * ================================================================
 */
@Repository
public interface WebsiteMediaUsageRepository
        extends JpaRepository<WebsiteMediaUsage, UUID> {

    /**
     * Retrieves one usage and its associated media asset.
     *
     * @param mediaUsageId media usage identifier
     * @return matching usage
     */
    @EntityGraph(attributePaths = "mediaAsset")
    Optional<WebsiteMediaUsage> findByMediaUsageId(
            UUID mediaUsageId
    );

    /**
     * Retrieves paginated references for one media asset.
     *
     * @param mediaAssetId media asset identifier
     * @param pageable pagination information
     * @return matching usage records
     */
    @EntityGraph(attributePaths = "mediaAsset")
    Page<WebsiteMediaUsage>
    findAllByMediaAsset_MediaAssetIdOrderByCreatedAtDesc(
            UUID mediaAssetId,
            Pageable pageable
    );

    /**
     * Retrieves all references for one media asset.
     *
     * This overload is intended for internal validation and reporting
     * operations that do not require pagination.
     *
     * @param mediaAssetId media asset identifier
     * @return matching usage records
     */
    @EntityGraph(attributePaths = "mediaAsset")
    List<WebsiteMediaUsage>
    findAllByMediaAsset_MediaAssetIdOrderByCreatedAtDesc(
            UUID mediaAssetId
    );

    /**
     * Retrieves all media references belonging to one CMS resource.
     *
     * @param resourceType resource domain
     * @param resourceId resource identifier
     * @return matching usage records
     */
    @EntityGraph(attributePaths = "mediaAsset")
    List<WebsiteMediaUsage>
    findAllByResourceTypeAndResourceIdOrderByCreatedAtAsc(
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId
    );

    /**
     * Retrieves the current usage assigned to one resource field.
     *
     * The supplied SQL schema permits multiple media assets for the
     * same resource field when their media_asset_id values differ.
     * Therefore, this query must return a list rather than Optional.
     *
     * @param resourceType resource domain
     * @param resourceId resource identifier
     * @param usageField normalized usage-field key
     * @return matching usage records
     */
    @EntityGraph(attributePaths = "mediaAsset")
    List<WebsiteMediaUsage>
    findAllByResourceTypeAndResourceIdAndUsageFieldOrderByCreatedAtAsc(
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId,
            String usageField
    );

    /**
     * Retrieves one exact media-to-resource reference.
     *
     * @param mediaAssetId media asset identifier
     * @param resourceType resource domain
     * @param resourceId resource identifier
     * @param usageField normalized usage field
     * @return exact matching usage
     */
    @EntityGraph(attributePaths = "mediaAsset")
    Optional<WebsiteMediaUsage>
    findByMediaAsset_MediaAssetIdAndResourceTypeAndResourceIdAndUsageField(
            UUID mediaAssetId,
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId,
            String usageField
    );

    /**
     * Checks whether an exact media usage already exists.
     */
    boolean existsByMediaAsset_MediaAssetIdAndResourceTypeAndResourceIdAndUsageField(
            UUID mediaAssetId,
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId,
            String usageField
    );

    /**
     * Checks whether any resource currently references a media asset.
     */
    boolean existsByMediaAsset_MediaAssetId(
            UUID mediaAssetId
    );

    /**
     * Counts all references to one media asset.
     */
    long countByMediaAsset_MediaAssetId(
            UUID mediaAssetId
    );

    /**
     * Counts all media references belonging to one resource.
     */
    long countByResourceTypeAndResourceId(
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId
    );

    /**
     * Deletes all references belonging to one resource.
     *
     * Used when an owning resource or immutable resource version is
     * removed.
     */
    long deleteAllByResourceTypeAndResourceId(
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId
    );

    /**
     * Deletes all references assigned to one resource field.
     *
     * This supports fields that may legitimately contain several media
     * assets, such as a gallery or image collection.
     */
    long deleteAllByResourceTypeAndResourceIdAndUsageField(
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId,
            String usageField
    );

    /**
     * Deletes all references to one media asset.
     *
     * This must only be used by a controlled force-cleanup operation.
     */
    long deleteAllByMediaAsset_MediaAssetId(
            UUID mediaAssetId
    );
}