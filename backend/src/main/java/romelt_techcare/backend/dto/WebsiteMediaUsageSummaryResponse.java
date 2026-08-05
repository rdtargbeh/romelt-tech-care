package romelt_techcare.backend.dto;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA USAGE SUMMARY RESPONSE
 * ================================================================
 *
 * Purpose:
 * Summarizes whether a media asset is currently referenced and can
 * safely be deleted.
 *
 * Responsibilities:
 * - Returns the media asset identifier.
 * - Returns the current reference count.
 * - Indicates whether the media is in use.
 * - Indicates whether deletion is currently permitted.
 *
 * Important:
 * This response reports CMS reference state only. File-storage rules,
 * legal retention, upload-processing state, and administrator
 * authorization may impose additional deletion restrictions.
 * ================================================================
 */
public record WebsiteMediaUsageSummaryResponse(

        UUID mediaAssetId,

        long usageCount,

        boolean inUse,

        boolean deletionAllowed
) {
}