package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION EVENT TYPE
 * ================================================================
 *
 * Purpose:
 * Identifies the business action that caused a notification event.
 *
 * Important:
 * Business services create these events inside their existing
 * database transaction. A separate notification worker processes the
 * committed events afterward.
 * ================================================================
 */
public enum NotificationEventType {

    BOOKING_REQUEST_RECEIVED,
    BOOKING_UNDER_REVIEW,
    BOOKING_CONFIRMED,
    BOOKING_COMPLETED,
    BOOKING_CANCELLED,
    BOOKING_DECLINED,
    BOOKING_EXPIRED,

    CONTACT_INQUIRY_RECEIVED,
    CONTACT_INQUIRY_RESPONDED,
    CONTACT_INQUIRY_CLOSED,

    REVIEW_INVITATION_CREATED,
    REVIEW_INVITATION_SENT,
    REVIEW_INVITATION_RESENT,
    REVIEW_INVITATION_REVOKED,
    REVIEW_INVITATION_EXPIRED,

    CUSTOMER_REVIEW_SUBMITTED,
    CUSTOMER_REVIEW_APPROVED,
    CUSTOMER_REVIEW_REJECTED,
    CUSTOMER_REVIEW_HIDDEN,
    CUSTOMER_REVIEW_ADMIN_RESPONSE_ADDED,

    CUSTOMER_WELCOME,
    CUSTOMER_PROFILE_UPDATED,

    GENERAL_CUSTOMER_NOTIFICATION,
    GENERAL_ADMIN_NOTIFICATION,
    SYSTEM_NOTIFICATION,
    OTHER
}