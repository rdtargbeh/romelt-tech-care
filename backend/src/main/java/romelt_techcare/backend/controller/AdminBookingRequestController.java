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
import romelt_techcare.backend.service.BookingRequestService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN BOOKING REQUEST CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Allows authenticated administrators to manage customer booking
 * requests.
 *
 * Responsibilities:
 * - Returns paginated booking requests.
 * - Returns one complete booking request.
 * - Creates bookings for customers who call, email, or walk in.
 * - Updates booking lifecycle status.
 *
 * Authorization:
 * SUPER_ADMIN, ADMIN, and STAFF may access these endpoints when
 * authenticated.
 *
 * Endpoints:
 * GET   /api/v1/admin/booking-requests
 * GET   /api/v1/admin/booking-requests/{bookingRequestId}
 * POST  /api/v1/admin/booking-requests
 * PATCH /api/v1/admin/booking-requests/{bookingRequestId}/status
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/booking-requests")
@RequiredArgsConstructor
public class AdminBookingRequestController {

    private final BookingRequestService
            bookingRequestService;

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

    /**
     * Creates a booking for a customer.
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
                .status(HttpStatus.CREATED)
                .body(body);
    }

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