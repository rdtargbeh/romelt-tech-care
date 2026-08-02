package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE VERSION STATUS
 * ================================================================
 *
 * Purpose:
 * Defines the lifecycle state of a versioned website service.
 *
 * Lifecycle:
 * DRAFT -> PUBLISHED -> ARCHIVED
 *
 * Rules:
 * - Only DRAFT versions may be edited.
 * - A service may have only one DRAFT version.
 * - A service may have only one PUBLISHED version.
 * - Publishing a draft archives the previously published version.
 * - Archived versions are retained as immutable history.
 *
 * Database alignment:
 * Values must remain aligned with the
 * ck_website_service_version_status constraint.
 * ================================================================
 */
public enum WebsiteServiceVersionStatus {

    DRAFT,

    PUBLISHED,

    ARCHIVED
}