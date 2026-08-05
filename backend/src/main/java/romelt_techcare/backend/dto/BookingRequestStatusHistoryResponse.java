package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.BookingRequestStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING STATUS HISTORY RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns one immutable booking-status transition to the
 * administrator portal.
 *
 * Responsibilities:
 * - Identifies the booking status-history record.
 * - Shows the previous and new statuses.
 * - Shows the reason supplied for the transition.
 * - Shows the administrator responsible for the change.
 * - Shows the related notification event when one exists.
 * - Shows when the status transition occurred.
 *
 * Security:
 * This response is administrator-only and must never be exposed
 * through the public booking API.
 * ================================================================
 */
public record BookingRequestStatusHistoryResponse(

        UUID bookingStatusHistoryId,

        UUID bookingRequestId,

        BookingRequestStatus previousStatus,

        BookingRequestStatus newStatus,

        String changeReason,

        UUID changedByAdminUserId,

        String changedByAdminName,

        UUID notificationEventId,

        Instant changedAt
) {
}