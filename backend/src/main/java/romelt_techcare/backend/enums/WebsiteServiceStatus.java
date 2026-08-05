package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — WEBSITE SERVICE STATUS
 * ================================================================
 *
 * Purpose:
 * Defines the lifecycle status of a stable website service record.
 *
 * Database alignment:
 * Values must remain aligned with the
 * ck_website_service_status database constraint.
 * ================================================================
 */
public enum WebsiteServiceStatus {

    ACTIVE,

    INACTIVE,

    ARCHIVED
}