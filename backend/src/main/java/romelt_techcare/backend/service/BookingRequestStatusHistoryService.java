package romelt_techcare.backend.service;

import romelt_techcare.backend.dto.BookingRequestStatusHistoryResponse;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING STATUS HISTORY SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines read-only administrator operations for booking lifecycle
 * history.
 *
 * Responsibilities:
 * - Returns the complete status history for one booking.
 * - Verifies that the requested booking exists.
 * - Returns records in newest-first order.
 *
 * Important:
 * Status-history records are created internally by the booking
 * service. This service does not expose create, update, or delete
 * operations.
 * ================================================================
 */
public interface BookingRequestStatusHistoryService {

    /**
     * Returns all lifecycle transitions for one booking, ordered from
     * newest to oldest.
     *
     * @param bookingRequestId booking request identifier
     * @return immutable status-history responses
     */
    List<BookingRequestStatusHistoryResponse> getBookingStatusHistory(
            UUID bookingRequestId
    );
}