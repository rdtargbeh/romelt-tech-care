package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.*;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — BOOKING REQUEST SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines public and administrator operations for customer booking
 * requests.
 *
 * Responsibilities:
 * - Creates public website booking requests.
 * - Creates administrator-entered customer bookings.
 * - Returns paginated booking records to administrators.
 * - Returns one complete booking record to administrators.
 * ================================================================
 */
public interface BookingRequestService {

    /**
     * Creates a booking submitted by a customer through the public
     * website booking form.
     */
    BookingRequestConfirmationResponse createBookingRequest(
            BookingRequestCreateRequest request
    );

    /**
     * Creates a booking entered by an authenticated administrator for
     * a customer.
     */
    AdminBookingRequestResponse createAdminBookingRequest(
            AdminJwtPrincipal principal,
            AdminBookingRequestCreateRequest request
    );

    /**
     * Returns a paginated administrator booking list.
     */
    Page<AdminBookingRequestResponse> getAdminBookingRequests(
            Pageable pageable
    );

    /**
     * Returns one complete booking for the administrator portal.
     */
    AdminBookingRequestResponse getAdminBookingRequest(
            UUID bookingRequestId
    );

    /**
     * Updates the operational status of a booking request.
     */
    AdminBookingRequestResponse updateAdminBookingStatus(
            AdminJwtPrincipal principal,
            UUID bookingRequestId,
            AdminBookingStatusUpdateRequest request
    );
}