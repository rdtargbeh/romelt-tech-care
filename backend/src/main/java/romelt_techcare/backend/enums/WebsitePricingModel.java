package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE PRICING MODEL
 * ================================================================
 *
 * Purpose:
 * Defines how a public pricing plan is priced.
 *
 * Database alignment:
 * Values must remain aligned with
 * ck_website_pricing_version_model.
 * ================================================================
 */
public enum WebsitePricingModel {

    ONE_TIME,

    HOURLY,

    DAILY,

    WEEKLY,

    MONTHLY,

    QUARTERLY,

    ANNUAL,

    CUSTOM,

    CONTACT_FOR_PRICE
}