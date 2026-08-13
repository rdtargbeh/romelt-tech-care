package romelt_techcare.backend.event;

import romelt_techcare.backend.entity.BookingRequest;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING SUBMITTED EVENT
 * ================================================================
 *
 * Purpose:
 * Published after a booking request has been persisted successfully.
 *
 * Processing:
 * BookingNotificationListener receives this event after the booking
 * transaction commits and sends customer and administrator
 * notifications.
 * ================================================================
 */
public record BookingSubmittedEvent(

        BookingRequest bookingRequest
) {

    public BookingSubmittedEvent {
        if (
                bookingRequest == null
                        || bookingRequest.getBookingRequestId() == null
        ) {
            throw new IllegalArgumentException(
                    "A persisted booking request is required."
            );
        }
    }
}