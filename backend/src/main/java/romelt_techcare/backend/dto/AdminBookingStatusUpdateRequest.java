package romelt_techcare.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.BookingRequestStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN BOOKING STATUS UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Validates an administrator request to update the operational status
 * of an existing customer booking request.
 *
 * Responsibilities:
 * - Requires a valid BookingRequestStatus.
 * - Accepts an optional administrator note explaining the change.
 *
 * Endpoint:
 * PATCH /api/v1/admin/booking-requests/{bookingRequestId}/status
 * ================================================================
 */
public record AdminBookingStatusUpdateRequest(

        @NotNull(
                message = "Select a booking status."
        )
        BookingRequestStatus status,

        @Size(
                max = 2000,
                message = "Administrator notes cannot exceed 2,000 characters."
        )
        String adminNotes
) {
}