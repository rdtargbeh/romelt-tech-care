package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW SOURCE
 * ================================================================
 *
 * Purpose:
 * Identifies where a customer review originated.
 *
 * Database alignment:
 * Values must remain aligned with ck_customer_review_source.
 * ================================================================
 */
public enum CustomerReviewSource {

    WEBSITE,

    BOOKING_FOLLOW_UP,

    EMAIL,

    PHONE,

    GOOGLE,

    FACEBOOK,

    OTHER
}