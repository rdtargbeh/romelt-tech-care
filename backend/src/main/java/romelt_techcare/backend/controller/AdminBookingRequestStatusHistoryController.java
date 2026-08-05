package romelt_techcare.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import romelt_techcare.backend.dto.BookingRequestStatusHistoryResponse;
import romelt_techcare.backend.service.BookingRequestStatusHistoryService;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN BOOKING STATUS HISTORY CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes read-only booking lifecycle history to the authenticated
 * administrator portal.
 *
 * Responsibilities:
 * - Returns all status transitions for one booking.
 * - Preserves newest-first history ordering.
 * - Does not expose create, update, or delete endpoints.
 *
 * Endpoint:
 * GET
 * /api/v1/admin/booking-requests/{bookingRequestId}/status-history
 *
 * Security:
 * This controller must remain behind the existing administrator
 * authentication and authorization configuration.
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/booking-requests")
@RequiredArgsConstructor
public class AdminBookingRequestStatusHistoryController {

    private final BookingRequestStatusHistoryService
            bookingRequestStatusHistoryService;

    /**
     * Returns all status transitions for one booking.
     */
    @GetMapping("/{bookingRequestId}/status-history")
    public ResponseEntity<
            List<BookingRequestStatusHistoryResponse>
            > getBookingStatusHistory(
            @PathVariable UUID bookingRequestId
    ) {
        List<BookingRequestStatusHistoryResponse> response =
                bookingRequestStatusHistoryService
                        .getBookingStatusHistory(
                                bookingRequestId
                        );

        return ResponseEntity.ok(response);
    }
}