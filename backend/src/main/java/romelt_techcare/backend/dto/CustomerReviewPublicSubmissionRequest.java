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
 * ROMELT TECHCARE — PUBLIC REVIEW SUBMISSION REQUEST
 * ================================================================
 *
 * Purpose:
 * Carries a verified customer's review submitted through a secure
 * invitation token.
 *
 * Security:
 * The invitation token must be sent separately to the service and
 * must never be persisted in the customer_reviews table.
 * ================================================================
 */
public record CustomerReviewPublicSubmissionRequest(

        @NotBlank(message = "Review invitation token is required.")
        String token,

        @NotNull(message = "Display preference is required.")
        CustomerReviewDisplayPreference reviewerDisplayPreference,

        @Size(
                max = 180,
                message = "Reviewer display name must not exceed 180 characters."
        )
        String reviewerDisplayName,

        @Size(
                max = 255,
                message = "Review title must not exceed 255 characters."
        )
        String reviewTitle,

        @NotBlank(message = "Review text is required.")
        @Size(
                max = 10000,
                message = "Review text must not exceed 10,000 characters."
        )
        String reviewText,

        @NotNull(message = "Rating is required.")
        @Min(
                value = 1,
                message = "Rating must be at least 1."
        )
        @Max(
                value = 5,
                message = "Rating must not exceed 5."
        )
        Short rating,

        UUID serviceId,

        UUID customerPhotoMediaId,

        @AssertTrue(
                message = "Customer consent must be confirmed."
        )
        Boolean customerConsentConfirmed,

        @NotBlank(message = "Consent version is required.")
        @Size(
                max = 50,
                message = "Consent version must not exceed 50 characters."
        )
        String customerConsentVersion
) {
}