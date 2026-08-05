package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.BookingRequestStatus;
import romelt_techcare.backend.enums.PreferredServiceTime;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING CONFIRMATION RESPONSE
 * ================================================================
 *
 * Purpose:
 * Returns safe confirmation details after accepting a public booking
 * request.
 *
 * Important:
 * This response confirms receipt of the request only. It does not
 * confirm an appointment.
 * ================================================================
 */
public record BookingRequestConfirmationResponse(

        UUID bookingRequestId,

        String referenceNumber,

        String message,

        Instant submittedAt,

        LocalDate requestedDate,

        PreferredServiceTime requestedTime,

        BookingRequestStatus status
) {
}