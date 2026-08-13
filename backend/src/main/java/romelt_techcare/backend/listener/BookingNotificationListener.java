package romelt_techcare.backend.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import romelt_techcare.backend.event.BookingStatusChangedEvent;
import romelt_techcare.backend.event.BookingSubmittedEvent;
import romelt_techcare.backend.service.BookingNotificationService;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING NOTIFICATION LISTENER
 * ================================================================
 *
 * Purpose:
 * Starts booking notifications only after the related booking
 * transaction commits successfully.
 *
 * Why AFTER_COMMIT:
 * - A failed booking transaction must not send notifications.
 * - Email or SMS failure must not undo a valid booking.
 * - Administrator in-app notifications are created only for
 *   committed booking records.
 * ================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingNotificationListener {

    private final BookingNotificationService
            bookingNotificationService;

    /**
     * Processes a newly submitted booking after commit.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleBookingSubmitted(
            BookingSubmittedEvent event
    ) {
        if (event == null || event.bookingRequest() == null) {
            log.warn(
                    "Booking-submitted notification event was empty."
            );

            return;
        }

        try {
            bookingNotificationService.bookingSubmitted(
                    event.bookingRequest()
            );

        } catch (Exception exception) {
            log.error(
                    "Booking-submission notification processing failed. bookingRequestId={}, referenceNumber={}, errorType={}",
                    event.bookingRequest()
                            .getBookingRequestId(),
                    event.bookingRequest()
                            .getReferenceNumber(),
                    exception.getClass().getSimpleName(),
                    exception
            );
        }
    }

    /**
     * Processes a booking status change after commit.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleBookingStatusChanged(
            BookingStatusChangedEvent event
    ) {
        if (
                event == null
                        || event.bookingRequest() == null
                        || event.previousStatus() == null
        ) {
            log.warn(
                    "Booking-status notification event was incomplete."
            );

            return;
        }

        try {
            bookingNotificationService.bookingStatusChanged(
                    event.bookingRequest(),
                    event.previousStatus()
            );

        } catch (Exception exception) {
            log.error(
                    "Booking-status notification processing failed. bookingRequestId={}, referenceNumber={}, previousStatus={}, currentStatus={}, errorType={}",
                    event.bookingRequest()
                            .getBookingRequestId(),
                    event.bookingRequest()
                            .getReferenceNumber(),
                    event.previousStatus(),
                    event.bookingRequest()
                            .getStatus(),
                    exception.getClass().getSimpleName(),
                    exception
            );
        }
    }
}