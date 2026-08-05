package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE FAQ VERSION STATUS
 * ================================================================
 *
 * Purpose:
 * Defines the lifecycle state of versioned website FAQ content.
 *
 * Lifecycle:
 * - DRAFT: administrator-editable content.
 * - PUBLISHED: current public content.
 * - ARCHIVED: immutable historical content.
 *
 * Database alignment:
 * Values must remain aligned with the
 * ck_website_faq_version_status database constraint.
 * ================================================================
 */
public enum WebsiteFaqVersionStatus {

    DRAFT,

    PUBLISHED,

    ARCHIVED
}