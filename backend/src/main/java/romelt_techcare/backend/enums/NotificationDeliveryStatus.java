package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION DELIVERY STATUS
 * ================================================================
 *
 * Purpose:
 * Tracks the current lifecycle state of one notification delivery.
 *
 * PENDING:
 * The delivery is ready for initial processing.
 *
 * PROCESSING:
 * The delivery has been claimed by a worker.
 *
 * RETRY_PENDING:
 * A temporary failure occurred and another attempt is scheduled.
 *
 * SENT:
 * The external provider accepted the EMAIL or SMS message.
 *
 * DELIVERED:
 * The provider confirmed delivery, or an IN_APP notification was
 * published successfully in the administrator portal.
 *
 * OPENED:
 * An email was opened or an IN_APP notification was read.
 *
 * CLICKED:
 * A tracked link in an EMAIL or SMS notification was clicked.
 *
 * BOUNCED:
 * The provider reported that the EMAIL or SMS could not be delivered.
 *
 * FAILED:
 * The delivery permanently failed.
 *
 * CANCELLED:
 * The delivery was cancelled before successful completion.
 *
 * SUPPRESSED:
 * The delivery was blocked by suppression or opt-out rules.
 * ================================================================
 */
public enum NotificationDeliveryStatus {

    PENDING,

    PROCESSING,

    RETRY_PENDING,

    SENT,

    DELIVERED,

    OPENED,

    CLICKED,

    BOUNCED,

    FAILED,

    CANCELLED,

    SUPPRESSED
}