package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.ServiceMethod;

import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW ELIGIBLE BOOKING RESPONSE
 * ================================================================
 *
 * Purpose:
 * Provides the administrator frontend with completed bookings that
 * are eligible to receive a customer review.
 *
 * Core business rule:
 *
 * Customer
 *      -> BookingRequest
 *          -> WebsiteService
 *              -> CustomerReview
 *
 * A booking is eligible only when:
 * - booking status is COMPLETED;
 * - completedAt is populated;
 * - reviewEligible is true;
 * - customerId is populated;
 * - serviceId is populated;
 * - no CustomerReview already exists for the booking.
 *
 * This DTO is intended for the administrator review-create selector.
 *
 * It provides enough information for the administrator to recognize
 * the correct completed service without exposing the entire
 * BookingRequest entity.
 *
 * Real-data integration:
 *
 * GET /api/v1/admin/customer-reviews/eligible-bookings
 * ================================================================
 */
public record CustomerReviewEligibleBookingResponse(

        // =============================================================
        // BOOKING
        // =============================================================

        UUID bookingRequestId,

        String referenceNumber,

        // =============================================================
        // CUSTOMER
        // =============================================================

        UUID customerId,

        String customerNumber,

        String customerDisplayName,

        String customerEmail,

        String customerPhone,

        // =============================================================
        // SERVICE
        // =============================================================

        UUID serviceId,

        String serviceCode,

        String serviceSlug,

        String serviceType,

        ServiceMethod serviceMethod,

        // =============================================================
        // COMPLETION
        // =============================================================

        Instant completedAt,

        String completionSummary

) {
}