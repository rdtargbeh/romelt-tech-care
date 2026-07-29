package romelt_techcare.backend.enums;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING REQUEST STATUS
 * ================================================================
 *
 * Purpose:
 * Tracks the lifecycle of a customer booking request.
 *
 * Lifecycle:
 * PENDING → UNDER_REVIEW → CONFIRMED → COMPLETED
 *
 * A request may also become:
 * - CANCELLED
 * - DECLINED
 * - EXPIRED
 * ================================================================
 */
public enum BookingRequestStatus {

    PENDING,

    UNDER_REVIEW,

    CONFIRMED,

    COMPLETED,

    CANCELLED,

    DECLINED,

    EXPIRED
}