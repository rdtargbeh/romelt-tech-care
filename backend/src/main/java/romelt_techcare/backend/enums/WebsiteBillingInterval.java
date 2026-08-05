package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE BILLING INTERVAL
 * ================================================================
 *
 * Purpose:
 * Defines the billing interval displayed for a pricing plan.
 *
 * Database alignment:
 * Values must remain aligned with
 * ck_website_pricing_version_interval.
 * ================================================================
 */
public enum WebsiteBillingInterval {

    ONE_TIME,

    HOUR,

    DAY,

    WEEK,

    MONTH,

    QUARTER,

    YEAR,

    CUSTOM
}