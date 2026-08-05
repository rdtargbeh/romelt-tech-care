package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.entity.WebsiteMediaUsage;
import romelt_techcare.backend.enums.WebsiteMediaUsageResourceType;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA USAGE SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines system-managed operations for tracking where website media
 * assets are used throughout the Romelt TechCare CMS.
 *
 * Responsibilities:
 * - Registers exact media references.
 * - Replaces media assigned to a single-value resource field.
 * - Removes obsolete media references.
 * - Retrieves usage information for administrator inspection.
 * - Counts references before media deletion.
 * - Prevents deletion of media that remains in use.
 *
 * Ownership rule:
 * Usage records are not directly managed through arbitrary public or
 * administrator request payloads. The owning business-profile, page,
 * service, pricing, navigation, and review services must create and
 * remove usage records as part of their own transactions.
 *
 * Generic-resource rule:
 * resourceId can identify records from several different tables.
 * Therefore, the owning domain service must verify that its resource
 * exists before registering a usage reference.
 * ================================================================
 */
public interface WebsiteMediaUsageService {

    /**
     * Registers an exact media reference.
     *
     * Returns the existing record when the exact combination has
     * already been registered.
     */
    WebsiteMediaUsage registerUsage(
            UUID mediaAssetId,
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId,
            String usageField,
            String usageDescription
    );

    /**
     * Replaces every current media reference assigned to one
     * single-value resource field.
     *
     * Passing null for newMediaAssetId removes the existing field
     * reference without registering a replacement.
     */
    WebsiteMediaUsage replaceUsage(
            UUID newMediaAssetId,
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId,
            String usageField,
            String usageDescription
    );

    /**
     * Retrieves one usage record.
     */
    WebsiteMediaUsage getUsage(
            UUID mediaUsageId
    );

    /**
     * Retrieves paginated references for one media asset.
     */
    Page<WebsiteMediaUsage> getUsagesByMediaAsset(
            UUID mediaAssetId,
            Pageable pageable
    );

    /**
     * Retrieves all media references belonging to one resource.
     */
    List<WebsiteMediaUsage> getUsagesByResource(
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId
    );

    /**
     * Removes one exact usage record.
     */
    void removeUsage(
            UUID mediaUsageId
    );

    /**
     * Removes every media reference assigned to one resource field.
     */
    long removeUsagesByResourceField(
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId,
            String usageField
    );

    /**
     * Removes every media reference belonging to one resource.
     */
    long removeAllUsagesForResource(
            WebsiteMediaUsageResourceType resourceType,
            UUID resourceId
    );

    /**
     * Determines whether a media asset is referenced.
     */
    boolean isMediaAssetInUse(
            UUID mediaAssetId
    );

    /**
     * Counts references to a media asset.
     */
    long countUsages(
            UUID mediaAssetId
    );

    /**
     * Rejects deletion when references remain.
     */
    void requireMediaAssetNotInUse(
            UUID mediaAssetId
    );
}