package romelt_techcare.backend.event;

import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.enums.BookingRequestStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING STATUS CHANGED EVENT
 * ================================================================
 *
 * Purpose:
 * Published after a booking status change has been persisted.
 *
 * Processing:
 * BookingNotificationListener receives this event after the business
 * transaction commits.
 * ================================================================
 */
public record BookingStatusChangedEvent(

        BookingRequest bookingRequest,

        BookingRequestStatus previousStatus
) {

    public BookingStatusChangedEvent {
        if (
                bookingRequest == null
                        || bookingRequest.getBookingRequestId() == null
        ) {
            throw new IllegalArgumentException(
                    "A persisted booking request is required."
            );
        }

        if (previousStatus == null) {
            throw new IllegalArgumentException(
                    "Previous booking status is required."
            );
        }

        if (bookingRequest.getStatus() == null) {
            throw new IllegalArgumentException(
                    "Current booking status is required."
            );
        }
    }
}