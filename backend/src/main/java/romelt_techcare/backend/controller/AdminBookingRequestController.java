package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.AdminBookingRequestCreateRequest;
import romelt_techcare.backend.dto.AdminBookingRequestResponse;
import romelt_techcare.backend.dto.AdminBookingStatusUpdateRequest;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.BookingCustomerPrefillResponse;
import romelt_techcare.backend.service.BookingRequestService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN BOOKING REQUEST CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Allows authenticated administrators to manage customer booking
 * requests and retrieve reusable customer data needed to prefill the
 * Create Booking form.
 *
 * Responsibilities:
 * - Returns paginated booking requests.
 * - Returns one complete booking request.
 * - Returns booking-prefill information for an existing customer.
 * - Creates bookings for customers who call, email, or walk in.
 * - Updates booking lifecycle status.
 *
 * Existing-customer Create Booking workflow:
 *
 * Administrator searches/selects Customer
 *      -> customerId
 *          -> GET /customer-prefill/{customerId}
 *              -> reusable Customer fields returned
 *                  -> frontend fills Create Booking customer fields
 *                      -> POST booking with customerId
 *
 * Important:
 * The customer-prefill endpoint only returns reusable Customer data.
 * Booking-specific information such as:
 * - business snapshot;
 * - requested service;
 * - requested schedule;
 * - actual service location;
 * - device/problem information;
 * - administrator notes;
 *
 * remains part of the booking form.
 *
 * Authorization:
 * SUPER_ADMIN, ADMIN, and STAFF may access these endpoints when
 * authenticated.
 *
 * Endpoints:
 *
 * GET   /api/v1/admin/booking-requests
 *
 * GET   /api/v1/admin/booking-requests/customer-prefill/{customerId}
 *
 * GET   /api/v1/admin/booking-requests/{bookingRequestId}
 *
 * POST  /api/v1/admin/booking-requests
 *
 * PATCH /api/v1/admin/booking-requests/{bookingRequestId}/status
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/booking-requests")
@RequiredArgsConstructor
public class AdminBookingRequestController {

    private final BookingRequestService
            bookingRequestService;

    // =================================================================
    // CUSTOMER PREFILL
    // =================================================================

    /**
     * Returns reusable customer information for the administrator
     * Create Booking form.
     *
     * The frontend should call this endpoint after an existing Customer
     * has been selected from the customer search/dropdown.
     *
     * Example:
     *
     * GET
     * /api/v1/admin/booking-requests/customer-prefill/{customerId}
     *
     * Returned information may populate:
     * - full name;
     * - email;
     * - phone;
     * - preferred contact method;
     * - saved address defaults.
     *
     * The saved customer address is a frontend default only.
     * The administrator may change the booking service address because
     * a customer can request service at another location.
     */
    @GetMapping("/customer-prefill/{customerId}")
    public ResponseEntity<
            ApiResponse<BookingCustomerPrefillResponse>
            > getBookingCustomerPrefill(
            @PathVariable
            UUID customerId,

            HttpServletRequest httpRequest
    ) {
        BookingCustomerPrefillResponse response =
                bookingRequestService
                        .getBookingCustomerPrefill(
                                customerId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Customer booking information retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    // =================================================================
    // ADMIN BOOKING LIST
    // =================================================================

    /**
     * Returns booking requests from newest to oldest.
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<AdminBookingRequestResponse>>
            > getBookingRequests(
            @PageableDefault(
                    size = 10,
                    sort = "submittedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<AdminBookingRequestResponse> response =
                bookingRequestService
                        .getAdminBookingRequests(
                                pageable
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Booking requests retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    // =================================================================
    // ADMIN BOOKING GET ONE
    // =================================================================

    /**
     * Returns one complete booking request.
     */
    @GetMapping("/{bookingRequestId}")
    public ResponseEntity<
            ApiResponse<AdminBookingRequestResponse>
            > getBookingRequest(
            @PathVariable
            UUID bookingRequestId,

            HttpServletRequest httpRequest
    ) {
        AdminBookingRequestResponse response =
                bookingRequestService
                        .getAdminBookingRequest(
                                bookingRequestId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Booking request retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    // =================================================================
    // ADMIN BOOKING CREATE
    // =================================================================

    /**
     * Creates a booking for a customer.
     *
     * When request.customerId() is supplied, the backend resolves that
     * existing Customer and uses the current reusable Customer record as
     * the authoritative source for customer identity/contact snapshot
     * fields.
     *
     * When customerId is null, the existing customer-resolution process
     * may resolve or create the Customer from the supplied booking data.
     */
    @PostMapping
    public ResponseEntity<
            ApiResponse<AdminBookingRequestResponse>
            > createBookingRequest(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            AdminBookingRequestCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        AdminBookingRequestResponse response =
                bookingRequestService
                        .createAdminBookingRequest(
                                principal,
                                request
                        );

        ApiResponse<AdminBookingRequestResponse> body =
                ApiResponse.success(
                        "Customer booking created successfully.",
                        response,
                        httpRequest.getRequestURI()
                );

        return ResponseEntity
                .status(
                        HttpStatus.CREATED
                )
                .body(
                        body
                );
    }

    // =================================================================
    // ADMIN BOOKING STATUS
    // =================================================================

    /**
     * Updates the lifecycle status of an existing booking.
     */
    @PatchMapping("/{bookingRequestId}/status")
    public ResponseEntity<
            ApiResponse<AdminBookingRequestResponse>
            > updateBookingStatus(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID bookingRequestId,

            @Valid
            @RequestBody
            AdminBookingStatusUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        AdminBookingRequestResponse response =
                bookingRequestService
                        .updateAdminBookingStatus(
                                principal,
                                bookingRequestId,
                                request
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Booking status updated successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }
}