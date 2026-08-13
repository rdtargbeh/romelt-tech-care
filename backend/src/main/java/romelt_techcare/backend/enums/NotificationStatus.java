package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION STATUS
 * ================================================================
 *
 * Purpose:
 * Tracks the simplified notification lifecycle.
 *
 * PENDING:
 * The notification record exists but has not been submitted.
 *
 * SENT:
 * The email or SMS provider accepted the message.
 *
 * DELIVERED:
 * The provider confirmed delivery, or an IN_APP notification was
 * published to the administrator portal.
 *
 * FAILED:
 * Delivery failed.
 *
 * CANCELLED:
 * Delivery was intentionally cancelled.
 * ================================================================
 */
public enum NotificationStatus {

    PENDING,

    SENT,

    DELIVERED,

    FAILED,

    CANCELLED
}