package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — NOTIFICATION EVENT STATUS
 * ================================================================
 *
 * Purpose:
 * Tracks processing of the transactional notification outbox.
 *
 * Values:
 * - PENDING: Ready for initial worker processing.
 * - PROCESSING: Claimed by a worker under an active lease.
 * - RETRY_PENDING: Failed temporarily and scheduled for retry.
 * - PROCESSED: Deliveries were created successfully.
 * - FAILED: Permanently failed after maximum attempts.
 * - CANCELLED: Administratively or programmatically cancelled.
 * ================================================================
 */
public enum NotificationEventStatus {

    PENDING,

    PROCESSING,

    RETRY_PENDING,

    PROCESSED,

    FAILED,

    CANCELLED
}