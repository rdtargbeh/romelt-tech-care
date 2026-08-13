package romelt_techcare.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.CustomerReviewDisplayPreference;
import romelt_techcare.backend.enums.CustomerReviewSource;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER REVIEW CREATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Allows an authenticated administrator to record customer feedback
 * for a completed Romelt TechCare service.
 *
 * Core business rule:
 * Every customer review must be associated with a completed,
 * review-eligible booking representing a service actually received
 * by the customer.
 *
 * The review source identifies where the customer communicated the
 * feedback. Examples:
 * - BOOKING_FOLLOW_UP
 * - PHONE
 * - EMAIL
 * - GOOGLE
 * - FACEBOOK
 * - OTHER
 *
 * The review source does not establish customer verification.
 * Customer and service verification come from the associated
 * completed booking.
 *
 * Backend-derived data:
 * - Customer identity
 * - Customer email
 * - Customer telephone number
 * - Service
 * - Verified-customer status
 *
 * These values must not be supplied or overridden by the frontend.
 * ================================================================
 */
public record CustomerReviewAdminCreateRequest(

        @NotNull(message = "Completed booking request is required.")
        UUID bookingRequestId,

        @NotNull(message = "Reviewer display preference is required.")
        CustomerReviewDisplayPreference reviewerDisplayPreference,

        /**
         * Used primarily when the customer selected CUSTOM display.
         *
         * Normal customer identity is resolved from the booking's
         * linked customer record.
         */
        @Size(max = 180, message = "Reviewer display name cannot exceed 180 characters.")
        String reviewerDisplayName,

        @Size(max = 255, message = "Review title cannot exceed 255 characters.")
        String reviewTitle,

        @NotBlank(message = "Review text is required.")
        @Size(max = 10000, message = "Review text cannot exceed 10,000 characters.")
        String reviewText,

        @NotNull(message = "Rating is required.")
        @Min(value = 1, message = "Rating must be at least 1.")
        @Max(value = 5, message = "Rating must not exceed 5.")
        Short rating,

        /**
         * Describes where the customer communicated the feedback.
         *
         * The service itself is still verified through bookingRequestId.
         */
        @NotNull(message = "Review source is required.")
        CustomerReviewSource reviewSource,

        /**
         * Original external review location when applicable.
         *
         * Examples:
         * - Google review URL
         * - Facebook recommendation URL
         *
         * Normally null for PHONE, EMAIL, or BOOKING_FOLLOW_UP.
         */
        @Size(max = 1500, message = "External source URL cannot exceed 1,500 characters.")
        String externalSourceUrl,

        UUID customerPhotoMediaId,

        /**
         * Indicates that the customer authorized Romelt TechCare to
         * record/use the supplied feedback.
         */
        Boolean customerConsentConfirmed,

        @Size(max = 50, message = "Customer consent version cannot exceed 50 characters.")
        String customerConsentVersion

) {
}