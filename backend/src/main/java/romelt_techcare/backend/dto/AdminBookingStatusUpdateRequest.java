package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.BookingRequestStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN BOOKING STATUS UPDATE DTO
 * ================================================================
 *
 * Purpose:
 * Updates an existing booking's lifecycle state without replacing
 * the existing booking record or changing its submitted snapshots.
 *
 * Status-specific requirements:
 *
 * CONFIRMED:
 * - scheduledStartAt
 * - scheduledEndAt
 * - scheduledTimezone
 *
 * COMPLETED:
 * - completionSummary
 *
 * CANCELLED:
 * - cancellationReason
 *
 * DECLINED:
 * - declineReason
 *
 * EXPIRED:
 * - expirationReason
 * ================================================================
 */
public record AdminBookingStatusUpdateRequest(

        @NotNull(message = "Booking status is required.")
        BookingRequestStatus status,

        UUID assignedAdminUserId,

        Instant scheduledStartAt,

        Instant scheduledEndAt,

        @Size(
                max = 80,
                message = "Scheduled timezone cannot exceed 80 characters."
        )
        String scheduledTimezone,

        @Size(
                max = 500,
                message = "Cancellation reason cannot exceed 500 characters."
        )
        String cancellationReason,

        @Size(
                max = 500,
                message = "Decline reason cannot exceed 500 characters."
        )
        String declineReason,

        @Size(
                max = 500,
                message = "Expiration reason cannot exceed 500 characters."
        )
        String expirationReason,

        @Size(
                max = 10000,
                message = "Completion summary cannot exceed 10,000 characters."
        )
        String completionSummary,

        @Size(
                max = 20000,
                message = "Completion notes cannot exceed 20,000 characters."
        )
        String completionNotes,

        Boolean reviewEligible,

        @Size(
                max = 500,
                message = "Review eligibility notes cannot exceed 500 characters."
        )
        String reviewEligibilityNotes,

        @Size(
                max = 10000,
                message = "Administrator notes cannot exceed 10,000 characters."
        )
        String adminNotes,

        @Size(
                max = 500,
                message = "Status-change reason cannot exceed 500 characters."
        )
        String changeReason
) {
}