package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE MEDIA ASSET STATUS
 * ================================================================
 *
 * Purpose:
 * Defines the processing and lifecycle state of a media asset used
 * by the Romelt TechCare public website and CMS.
 *
 * Lifecycle:
 * UPLOADING → PROCESSING → ACTIVE
 *
 * Exceptional or terminal states:
 * - FAILED: File upload or media processing was unsuccessful.
 * - ARCHIVED: Asset is retained but should not be selected for new use.
 * - DELETED: Asset has been soft-deleted and is no longer available.
 *
 * Important:
 * DELETED represents a soft-deletion state. The database record and
 * audit information remain available until a separate permanent
 * cleanup process removes the stored file and metadata.
 * ================================================================
 */
public enum WebsiteMediaAssetStatus {

    /**
     * The file upload has started but is not yet complete.
     */
    UPLOADING,

    /**
     * The uploaded file is being validated, scanned, resized,
     * optimized, or otherwise processed.
     */
    PROCESSING,

    /**
     * The asset is valid and available for approved website use.
     */
    ACTIVE,

    /**
     * Uploading, validation, scanning, or processing failed.
     */
    FAILED,

    /**
     * The asset is retained for history but should not be selected
     * for new website content.
     */
    ARCHIVED,

    /**
     * The asset has been soft-deleted.
     */
    DELETED
}