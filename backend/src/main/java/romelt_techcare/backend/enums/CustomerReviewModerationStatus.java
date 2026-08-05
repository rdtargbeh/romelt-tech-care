package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW MODERATION STATUS
 * ================================================================
 *
 * Purpose:
 * Defines the moderation and publication lifecycle of a customer
 * review.
 *
 * Database alignment:
 * Values must remain aligned with ck_customer_review_status.
 * ================================================================
 */
public enum CustomerReviewModerationStatus {

    PENDING,

    APPROVED,

    REJECTED,

    HIDDEN,

    ARCHIVED,

    SPAM
}