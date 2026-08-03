package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW DISPLAY PREFERENCE
 * ================================================================
 *
 * Purpose:
 * Defines how a reviewer's identity is displayed publicly.
 *
 * Database alignment:
 * Values must remain aligned with
 * ck_customer_review_display_preference.
 * ================================================================
 */
public enum CustomerReviewDisplayPreference {

    FULL_NAME,

    FIRST_NAME_LAST_INITIAL,

    FIRST_NAME_ONLY,

    ANONYMOUS,

    CUSTOM
}