package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PRICING PLAN STATUS
 * ================================================================
 *
 * Purpose:
 * Defines the lifecycle status of a stable website pricing-plan
 * identity.
 *
 * Database alignment:
 * Values must remain aligned with the
 * ck_website_pricing_status database constraint.
 * ================================================================
 */
public enum WebsitePricingPlanStatus {

    ACTIVE,

    INACTIVE,

    ARCHIVED
}