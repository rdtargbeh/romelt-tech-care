package romelt_techcare.backend.mapper;

import org.springframework.stereotype.Component;
import romelt_techcare.backend.dto.WebsiteMediaUsageResponse;
import romelt_techcare.backend.dto.WebsiteMediaUsageSummaryResponse;
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.entity.WebsiteMediaUsage;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA USAGE MAPPER
 * ================================================================
 *
 * Purpose:
 * Converts WebsiteMediaUsage entities into safe administrator API
 * responses.
 *
 * Responsibilities:
 * - Maps usage identifiers and resource information.
 * - Maps limited media information needed for usage inspection.
 * - Produces deletion-eligibility summaries.
 * - Prevents controllers from serializing JPA entity graphs.
 *
 * Loading requirement:
 * Repository response-facing queries load mediaAsset using an entity
 * graph so this mapper does not trigger lazy-loading failures.
 * ================================================================
 */
@Component
public class WebsiteMediaUsageMapper {

    /**
     * Converts one usage entity into an administrator response.
     */
    public WebsiteMediaUsageResponse toResponse(
            WebsiteMediaUsage usage
    ) {
        if (usage == null) {
            return null;
        }

        WebsiteMediaAsset mediaAsset =
                usage.getMediaAsset();

        return new WebsiteMediaUsageResponse(
                usage.getMediaUsageId(),
                getMediaAssetId(mediaAsset),
                getAssetKey(mediaAsset),
                getMediaTitle(mediaAsset),
                getOriginalFileName(mediaAsset),
                getPublicUrl(mediaAsset),
                getMimeType(mediaAsset),
                mediaAsset == null
                        ? null
                        : mediaAsset.getAssetStatus(),
                mediaAsset == null
                        ? null
                        : mediaAsset.getIsPublic(),
                usage.getResourceType(),
                usage.getResourceId(),
                usage.getUsageField(),
                usage.getUsageDescription(),
                usage.getCreatedAt()
        );
    }

    /**
     * Creates a media usage summary.
     */
    public WebsiteMediaUsageSummaryResponse toSummaryResponse(
            UUID mediaAssetId,
            long usageCount
    ) {
        boolean inUse = usageCount > 0;

        return new WebsiteMediaUsageSummaryResponse(
                mediaAssetId,
                usageCount,
                inUse,
                !inUse
        );
    }

    private UUID getMediaAssetId(
            WebsiteMediaAsset mediaAsset
    ) {
        return mediaAsset == null
                ? null
                : mediaAsset.getMediaAssetId();
    }

    private String getAssetKey(
            WebsiteMediaAsset mediaAsset
    ) {
        return mediaAsset == null
                ? null
                : mediaAsset.getAssetKey();
    }

    private String getMediaTitle(
            WebsiteMediaAsset mediaAsset
    ) {
        return mediaAsset == null
                ? null
                : mediaAsset.getTitle();
    }

    private String getOriginalFileName(
            WebsiteMediaAsset mediaAsset
    ) {
        return mediaAsset == null
                ? null
                : mediaAsset.getOriginalFileName();
    }

    private String getPublicUrl(
            WebsiteMediaAsset mediaAsset
    ) {
        return mediaAsset == null
                ? null
                : mediaAsset.getPublicUrl();
    }

    private String getMimeType(
            WebsiteMediaAsset mediaAsset
    ) {
        return mediaAsset == null
                ? null
                : mediaAsset.getMimeType();
    }
}