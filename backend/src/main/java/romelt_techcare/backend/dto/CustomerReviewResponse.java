package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.CustomerReviewDisplayPreference;
import romelt_techcare.backend.enums.CustomerReviewModerationStatus;
import romelt_techcare.backend.enums.CustomerReviewSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW RESPONSE
 * ================================================================
 *
 * Purpose:
 * Complete administrator-facing representation of a customer review.
 *
 * Core business rule:
 * Every customer review belongs to an actual completed,
 * review-eligible BookingRequest and the WebsiteService performed
 * through that booking.
 *
 * Review ownership:
 *
 * Customer
 *      -> BookingRequest
 *          -> WebsiteService
 *              -> CustomerReview
 *
 * ContactInquiry is intentionally not part of this response because
 * an inquiry does not establish that a customer received a service.
 *
 * Security:
 * This administrator response may include operational review data,
 * customer contact snapshots, moderation details, consent evidence,
 * audit information, and internal lifecycle metadata.
 *
 * Public website responses must continue using
 * PublicCustomerReviewResponse instead of this DTO.
 * ================================================================
 */
public record CustomerReviewResponse(

        UUID customerReviewId,

        // =============================================================
        // REVIEW RELATIONSHIPS
        // =============================================================

        UUID reviewInvitationId,

        UUID bookingRequestId,

        String bookingReferenceNumber,

        UUID serviceId,

        String serviceCode,

        String serviceSlug,

        // =============================================================
        // REVIEWER IDENTITY
        // =============================================================

        String reviewerDisplayName,

        String resolvedPublicDisplayName,

        CustomerReviewDisplayPreference reviewerDisplayPreference,

        /**
         * Customer email snapshot captured by the backend from the
         * reusable Customer associated with the completed booking.
         */
        String reviewerEmail,

        /**
         * Customer telephone snapshot captured by the backend from the
         * reusable Customer associated with the completed booking.
         */
        String reviewerPhone,

        // =============================================================
        // REVIEW CONTENT
        // =============================================================

        String reviewTitle,

        String reviewText,

        Short rating,

        CustomerReviewSource reviewSource,

        String externalSourceUrl,

        PublicWebsiteMediaAssetResponse customerPhoto,

        // =============================================================
        // CUSTOMER VERIFICATION
        // =============================================================

        Boolean isVerifiedCustomer,

        // =============================================================
        // CUSTOMER CONSENT
        // =============================================================

        Boolean customerConsentConfirmed,

        Instant customerConsentConfirmedAt,

        String customerConsentVersion,

        String customerConsentIpAddress,

        // =============================================================
        // MODERATION
        // =============================================================

        CustomerReviewModerationStatus moderationStatus,

        String moderationNotes,

        String rejectionReason,

        // =============================================================
        // PUBLICATION
        // =============================================================

        Boolean isPublic,

        Boolean isFeatured,

        Boolean publiclyVisible,

        // =============================================================
        // ADMINISTRATOR RESPONSE
        // =============================================================

        String adminResponse,

        Instant respondedAt,

        UUID respondedByAdminUserId,

        String respondedByAdminUserDisplayName,

        // =============================================================
        // SUBMISSION AUDIT
        // =============================================================

        String submissionIpAddress,

        String submissionUserAgent,

        // =============================================================
        // SPAM
        // =============================================================

        BigDecimal spamScore,

        Boolean isSpam,

        // =============================================================
        // REVIEW LIFECYCLE
        // =============================================================

        Instant submittedAt,

        Instant moderatedAt,

        Instant publishedAt,

        Instant hiddenAt,

        Instant archivedAt,

        // =============================================================
        // ADMINISTRATOR AUDIT
        // =============================================================

        UUID createdByAdminUserId,

        String createdByAdminUserDisplayName,

        UUID updatedByAdminUserId,

        String updatedByAdminUserDisplayName,

        UUID moderatedByAdminUserId,

        String moderatedByAdminUserDisplayName,

        UUID publishedByAdminUserId,

        String publishedByAdminUserDisplayName,

        UUID hiddenByAdminUserId,

        String hiddenByAdminUserDisplayName,

        UUID archivedByAdminUserId,

        String archivedByAdminUserDisplayName,

        // =============================================================
        // ENTITY AUDIT
        // =============================================================

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion

) {
}