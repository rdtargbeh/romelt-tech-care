package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PAGE VERSION STATUS
 * ================================================================
 *
 * Purpose:
 * Defines the lifecycle state of a website page version.
 *
 * Lifecycle:
 * DRAFT → PUBLISHED → ARCHIVED
 *
 * Rules:
 * - Only DRAFT versions may be edited.
 * - Only one DRAFT may exist for a page.
 * - Only one PUBLISHED version may exist for a page.
 * - Publishing a draft archives the previously published version.
 * - Archived versions are retained as immutable history.
 * ================================================================
 */
public enum WebsitePageVersionStatus {

    DRAFT,

    PUBLISHED,

    ARCHIVED
}