package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW INVITATION STATUS
 * ================================================================
 *
 * Purpose:
 * Defines the lifecycle state of a verified customer-review
 * invitation.
 *
 * Database alignment:
 * Values must remain aligned with
 * ck_review_invitation_status.
 * ================================================================
 */
public enum CustomerReviewInvitationStatus {

    PENDING,

    SENT,

    USED,

    EXPIRED,

    REVOKED
}