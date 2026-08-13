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
 * ROMELT TECHCARE — CUSTOMER REVIEW UPDATE REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries administrator-editable content for an existing customer
 * review.
 *
 * Core business rule:
 * A Romelt TechCare customer review represents feedback about an
 * actual service received through a completed BookingRequest.
 *
 * Once the review has been created, the factual service relationship
 * must remain immutable.
 *
 * Therefore this request does NOT allow administrators to change:
 * - bookingRequestId;
 * - customer identity;
 * - reviewer email;
 * - reviewer telephone number;
 * - serviceId;
 * - verified-customer status.
 *
 * Those values are derived from and protected by the completed
 * booking/customer relationship.
 *
 * Editable review information:
 * - public display preference;
 * - optional custom display name;
 * - title;
 * - review text;
 * - rating;
 * - review source;
 * - external source URL;
 * - optional customer photo.
 *
 * Review source:
 * reviewSource describes WHERE the customer communicated the
 * feedback.
 *
 * Examples:
 * - BOOKING_FOLLOW_UP
 * - PHONE
 * - EMAIL
 * - GOOGLE
 * - FACEBOOK
 * - OTHER
 *
 * Changing reviewSource does not change the underlying booking,
 * customer, service, or verified-customer status.
 *
 * External source:
 * externalSourceUrl may be used when the feedback originated from a
 * supported external channel such as Google or Facebook.
 *
 * Security:
 * The frontend must never be allowed to replace authoritative
 * booking, customer, or service identity through a review update.
 * ================================================================
 */
public record CustomerReviewUpdateRequest(

        /**
         * Public/customer-authorized review display name.
         *
         * Primarily used when reviewerDisplayPreference is CUSTOM.
         * For the standard name-display options, the service may
         * continue using the customer name already stored on the
         * review.
         */
        @Size(
                max = 180,
                message = "Reviewer display name cannot exceed 180 characters."
        )
        String reviewerDisplayName,

        @NotNull(
                message = "Reviewer display preference is required."
        )
        CustomerReviewDisplayPreference reviewerDisplayPreference,

        @Size(
                max = 255,
                message = "Review title cannot exceed 255 characters."
        )
        String reviewTitle,

        @NotBlank(
                message = "Review text is required."
        )
        @Size(
                max = 10000,
                message = "Review text cannot exceed 10,000 characters."
        )
        String reviewText,

        @NotNull(
                message = "Rating is required."
        )
        @Min(
                value = 1,
                message = "Rating must be at least 1."
        )
        @Max(
                value = 5,
                message = "Rating must not exceed 5."
        )
        Short rating,

        /**
         * Identifies where the customer communicated the review.
         *
         * This does not establish service ownership. The completed
         * BookingRequest remains the authoritative service record.
         */
        @NotNull(
                message = "Review source is required."
        )
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
        @Size(
                max = 1500,
                message = "External source URL cannot exceed 1,500 characters."
        )
        String externalSourceUrl,

        /**
         * Optional public customer-review media asset.
         *
         * Changing the photo does not change the customer/service
         * relationship represented by the review.
         */
        UUID customerPhotoMediaId

) {
}