package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.CustomerReviewRatingSummaryResponse;
import romelt_techcare.backend.entity.CustomerReview;
import romelt_techcare.backend.enums.CustomerReviewModerationStatus;
import romelt_techcare.backend.enums.CustomerReviewSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Defines customer-review submission, moderation, publication, and
 * public retrieval operations.
 */
public interface CustomerReviewService {

    CustomerReview submitVerifiedReview(
            String invitationToken,
            CustomerReview requestedReview,
            UUID serviceId,
            UUID customerPhotoMediaId,
            String consentVersion,
            String submissionIpAddress,
            String submissionUserAgent
    );

    CustomerReview createReviewByAdministrator(
            CustomerReview requestedReview,
            UUID bookingRequestId,
            UUID contactInquiryId,
            UUID serviceId,
            UUID customerPhotoMediaId,
            UUID administratorId
    );

    CustomerReview updateReview(
            UUID customerReviewId,
            CustomerReview requestedUpdate,
            UUID serviceId,
            UUID customerPhotoMediaId,
            UUID administratorId
    );

    CustomerReview getReview(
            UUID customerReviewId
    );

    Page<CustomerReview> searchReviews(
            String keyword,
            CustomerReviewModerationStatus moderationStatus,
            CustomerReviewSource reviewSource,
            Short rating,
            UUID serviceId,
            Boolean isVerifiedCustomer,
            Boolean isPublic,
            Boolean isFeatured,
            Boolean isSpam,
            Pageable pageable
    );

    CustomerReview approveReview(
            UUID customerReviewId,
            String moderationNotes,
            UUID administratorId
    );

    CustomerReview rejectReview(
            UUID customerReviewId,
            String rejectionReason,
            String moderationNotes,
            UUID administratorId
    );

    CustomerReview markReviewAsSpam(
            UUID customerReviewId,
            BigDecimal spamScore,
            String moderationNotes,
            UUID administratorId
    );

    CustomerReview publishReview(
            UUID customerReviewId,
            boolean featured,
            UUID administratorId
    );

    CustomerReview unpublishReview(
            UUID customerReviewId,
            UUID administratorId
    );

    CustomerReview updateFeaturedStatus(
            UUID customerReviewId,
            boolean featured,
            UUID administratorId
    );

    CustomerReview hideReview(
            UUID customerReviewId,
            String moderationNotes,
            UUID administratorId
    );

    CustomerReview archiveReview(
            UUID customerReviewId,
            UUID administratorId
    );

    CustomerReview addAdminResponse(
            UUID customerReviewId,
            String response,
            UUID administratorId
    );

    CustomerReview removeAdminResponse(
            UUID customerReviewId,
            UUID administratorId
    );

    Page<CustomerReview> getPublicReviews(
            Pageable pageable
    );

    List<CustomerReview> getFeaturedPublicReviews();

    Page<CustomerReview> getPublicReviewsByServiceSlug(
            String serviceSlug,
            Pageable pageable
    );

    CustomerReviewRatingSummaryResponse
    getPublicRatingSummary();
}