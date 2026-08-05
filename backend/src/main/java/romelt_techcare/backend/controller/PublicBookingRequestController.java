package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.BookingRequestConfirmationResponse;
import romelt_techcare.backend.dto.BookingRequestCreateRequest;
import romelt_techcare.backend.service.BookingRequestService;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC BOOKING REQUEST CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes the public endpoint used by the website booking form.
 *
 * Endpoint:
 * POST /api/v1/public/booking-requests
 *
 * Security:
 * - This endpoint is public.
 * - Authentication is not required.
 * - Server-side validation is always required.
 * - Production deployment should include rate limiting and spam
 *   prevention.
 * ================================================================
 */
@RestController
@RequestMapping(
        "/api/v1/public/booking-requests"
)
@RequiredArgsConstructor
public class PublicBookingRequestController {

    private final BookingRequestService
            bookingRequestService;

    @PostMapping
    public ResponseEntity<
            ApiResponse<
                    BookingRequestConfirmationResponse
                    >
            > createBookingRequest(
            @Valid
            @RequestBody
            BookingRequestCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        BookingRequestConfirmationResponse response =
                bookingRequestService
                        .createBookingRequest(request);

        ApiResponse<
                BookingRequestConfirmationResponse
                > body = ApiResponse.success(
                "Booking request submitted successfully.",
                response,
                httpRequest.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(body);
    }
}