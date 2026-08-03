package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — PRICING PLAN VERSION STATUS
 * ================================================================
 *
 * Purpose:
 * Defines the lifecycle state of versioned website pricing-plan
 * content.
 *
 * Lifecycle:
 * DRAFT -> PUBLISHED -> ARCHIVED
 *
 * Rules:
 * - Only DRAFT versions may be edited.
 * - A pricing plan may have only one DRAFT version.
 * - A pricing plan may have only one PUBLISHED version.
 * - Publishing a draft archives the previous published version.
 * - Archived versions remain immutable historical records.
 *
 * Database alignment:
 * Values must remain aligned with
 * ck_website_pricing_version_status.
 * ================================================================
 */
public enum WebsitePricingPlanVersionStatus {

    DRAFT,

    PUBLISHED,

    ARCHIVED
}