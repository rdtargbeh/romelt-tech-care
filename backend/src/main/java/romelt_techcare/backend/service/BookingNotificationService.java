package romelt_techcare.backend.service;

import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.enums.BookingRequestStatus;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING NOTIFICATION SERVICE
 * ================================================================
 *
 * Purpose:
 * Contains every notification rule related to booking requests.
 *
 * Responsibilities:
 * - Customer confirmation
 * - Customer status updates
 * - Administrator notifications
 * - SMS decisions
 * - Email decisions
 *
 * This service contains BUSINESS RULES only.
 *
 * Actual delivery is delegated to NotificationService.
 * ================================================================
 */
public interface BookingNotificationService {

    /**
     * Called immediately after a booking is successfully created.
     */
    void bookingSubmitted(
            BookingRequest bookingRequest
    );

    /**
     * Called whenever the booking status changes.
     */
    void bookingStatusChanged(
            BookingRequest bookingRequest,
            BookingRequestStatus previousStatus
    );

}