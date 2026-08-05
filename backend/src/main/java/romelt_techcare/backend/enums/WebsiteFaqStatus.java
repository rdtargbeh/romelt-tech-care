package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE FAQ STATUS
 * ================================================================
 *
 * Purpose:
 * Defines the lifecycle status of a stable website FAQ identity.
 *
 * Database alignment:
 * Values must remain aligned with the ck_website_faq_status
 * database constraint.
 * ================================================================
 */
public enum WebsiteFaqStatus {

    ACTIVE,

    INACTIVE,

    ARCHIVED
}