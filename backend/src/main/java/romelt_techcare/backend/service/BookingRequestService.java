package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.AdminBookingRequestCreateRequest;
import romelt_techcare.backend.dto.AdminBookingRequestResponse;
import romelt_techcare.backend.dto.AdminBookingStatusUpdateRequest;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.BookingCustomerPrefillResponse;
import romelt_techcare.backend.dto.BookingRequestConfirmationResponse;
import romelt_techcare.backend.dto.BookingRequestCreateRequest;

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
 * - Returns reusable customer data for administrator booking prefill.
 * - Returns paginated booking records to administrators.
 * - Returns one complete booking record to administrators.
 * - Updates booking lifecycle status.
 *
 * Existing-customer booking workflow:
 *
 * Administrator searches/selects Customer
 *      -> customerId
 *          -> getBookingCustomerPrefill(customerId)
 *              -> frontend fills reusable customer fields
 *                  -> administrator completes booking-specific fields
 *                      -> createAdminBookingRequest(...)
 *
 * Important:
 * The prefill operation exists for administrator convenience.
 *
 * The create operation must still resolve the selected Customer by
 * customerId and treat that reusable Customer record as authoritative
 * for customer-derived booking snapshot fields.
 * ================================================================
 */
public interface BookingRequestService {

    // =================================================================
    // PUBLIC CREATE
    // =================================================================

    /**
     * Creates a booking submitted by a customer through the public
     * website booking form.
     */
    BookingRequestConfirmationResponse createBookingRequest(
            BookingRequestCreateRequest request
    );

    // =================================================================
    // ADMIN CUSTOMER PREFILL
    // =================================================================

    /**
     * Returns current reusable Customer information needed to prefill
     * the administrator Create Booking form.
     *
     * The selected Customer must:
     * - exist;
     * - not be soft deleted;
     * - not be merged into another customer;
     * - be usable for a new booking.
     *
     * This operation does not create or modify a booking.
     *
     * @param customerId selected reusable Customer ID
     * @return booking-specific customer prefill information
     */
    BookingCustomerPrefillResponse getBookingCustomerPrefill(
            UUID customerId
    );

    // =================================================================
    // ADMIN CREATE
    // =================================================================

    /**
     * Creates a booking entered by an authenticated administrator for
     * a customer.
     *
     * If request.customerId() is supplied, the selected existing
     * Customer is authoritative for reusable customer identity and
     * contact information.
     *
     * If customerId is not supplied, the existing customer-resolution
     * workflow may resolve or create the reusable Customer.
     */
    AdminBookingRequestResponse createAdminBookingRequest(
            AdminJwtPrincipal principal,
            AdminBookingRequestCreateRequest request
    );

    // =================================================================
    // ADMIN LIST
    // =================================================================

    /**
     * Returns a paginated administrator booking list.
     */
    Page<AdminBookingRequestResponse> getAdminBookingRequests(
            Pageable pageable
    );

    // =================================================================
    // ADMIN GET ONE
    // =================================================================

    /**
     * Returns one complete booking for the administrator portal.
     */
    AdminBookingRequestResponse getAdminBookingRequest(
            UUID bookingRequestId
    );

    // =================================================================
    // ADMIN STATUS UPDATE
    // =================================================================

    /**
     * Updates the operational status of a booking request.
     */
    AdminBookingRequestResponse updateAdminBookingStatus(
            AdminJwtPrincipal principal,
            UUID bookingRequestId,
            AdminBookingStatusUpdateRequest request
    );
}