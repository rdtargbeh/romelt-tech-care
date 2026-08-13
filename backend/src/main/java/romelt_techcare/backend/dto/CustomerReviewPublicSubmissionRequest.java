package romelt_techcare.backend.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import romelt_techcare.backend.enums.CustomerReviewDisplayPreference;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC CUSTOMER REVIEW SUBMISSION REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries a verified customer's review submitted through a secure
 * review-invitation token after a completed Romelt TechCare service.
 *
 * Core business rule:
 * A public customer review represents feedback about the actual
 * completed service associated with the secure invitation.
 *
 * The frontend does NOT supply:
 * - bookingRequestId;
 * - customerId;
 * - serviceId;
 * - customer email;
 * - customer telephone number;
 * - verified-customer status;
 * - review source.
 *
 * Those values are resolved by the backend from:
 *
 * invitation
 *      -> BookingRequest
 *          -> customerId
 *          -> serviceId
 *
 * Review source:
 * Reviews submitted through this secure public invitation workflow
 * are assigned BOOKING_FOLLOW_UP by the backend.
 *
 * Security:
 * - The invitation token is used only to validate and resolve the
 *   invitation.
 * - The plain invitation token must never be persisted in the
 *   customer_reviews table.
 * - Customer/service identity must never be trusted from browser-
 *   supplied identifiers.
 * - Public submissions cannot control moderation or publication.
 *
 * Consent:
 * Customer consent must be explicitly confirmed before submission.
 * ================================================================
 */
public record CustomerReviewPublicSubmissionRequest(

        @NotBlank(
                message = "Review invitation token is required."
        )
        String token,

        @NotNull(
                message = "Display preference is required."
        )
        CustomerReviewDisplayPreference reviewerDisplayPreference,

        /**
         * Optional customer-selected display name.
         *
         * This may be used for CUSTOM display preference.
         *
         * For standard name-display preferences, the backend may use
         * the customer identity already associated with the completed
         * booking.
         */
        @Size(max = 180, message = "Reviewer display name must not exceed 180 characters.")
        String reviewerDisplayName,

        @Size(max = 255, message = "Review title must not exceed 255 characters.")
        String reviewTitle,

        @NotBlank(message = "Review text is required.")
        @Size(max = 10000, message = "Review text must not exceed 10,000 characters.")
        String reviewText,

        @NotNull(message = "Rating is required.")
        @Min(value = 1, message = "Rating must be at least 1.")
        @Max(value = 5, message = "Rating must not exceed 5.")
        Short rating,

        /**
         * Optional customer-review media asset.
         *
         * This does not identify the service being reviewed.
         */
        UUID customerPhotoMediaId,

        @AssertTrue(message = "Customer consent must be confirmed.")
        Boolean customerConsentConfirmed,

        @NotBlank(message = "Consent version is required.")
        @Size(max = 50, message = "Consent version must not exceed 50 characters.")
        String customerConsentVersion

) {
}