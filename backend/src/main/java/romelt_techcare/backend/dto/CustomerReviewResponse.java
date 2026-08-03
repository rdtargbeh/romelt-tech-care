package romelt_techcare.backend.dto;

import romelt_techcare.backend.enums.CustomerReviewDisplayPreference;
import romelt_techcare.backend.enums.CustomerReviewModerationStatus;
import romelt_techcare.backend.enums.CustomerReviewSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Complete administrator-facing customer-review response.
 */
public record CustomerReviewResponse(

        UUID customerReviewId,

        UUID reviewInvitationId,

        UUID bookingRequestId,

        String bookingReferenceNumber,

        UUID contactInquiryId,

        UUID serviceId,

        String serviceCode,

        String serviceSlug,

        String reviewerDisplayName,

        String resolvedPublicDisplayName,

        CustomerReviewDisplayPreference reviewerDisplayPreference,

        String reviewerEmail,

        String reviewerPhone,

        String reviewTitle,

        String reviewText,

        Short rating,

        CustomerReviewSource reviewSource,

        String externalSourceUrl,

        PublicWebsiteMediaAssetResponse customerPhoto,

        Boolean isVerifiedCustomer,

        Boolean customerConsentConfirmed,

        Instant customerConsentConfirmedAt,

        String customerConsentVersion,

        String customerConsentIpAddress,

        CustomerReviewModerationStatus moderationStatus,

        String moderationNotes,

        String rejectionReason,

        Boolean isPublic,

        Boolean isFeatured,

        Boolean publiclyVisible,

        String adminResponse,

        Instant respondedAt,

        UUID respondedByAdminUserId,

        String respondedByAdminUserDisplayName,

        String submissionIpAddress,

        String submissionUserAgent,

        BigDecimal spamScore,

        Boolean isSpam,

        Instant submittedAt,

        Instant moderatedAt,

        Instant publishedAt,

        Instant hiddenAt,

        Instant archivedAt,

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

        Instant createdAt,

        Instant updatedAt,

        Long rowVersion
) {
}