package romelt_techcare.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import romelt_techcare.backend.dto.CustomerReviewEligibleBookingResponse;
import romelt_techcare.backend.dto.CustomerReviewRatingSummaryResponse;
import romelt_techcare.backend.entity.CustomerReview;
import romelt_techcare.backend.enums.CustomerReviewModerationStatus;
import romelt_techcare.backend.enums.CustomerReviewSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW SERVICE
 * ================================================================
 *
 * Purpose:
 * Defines customer-review submission, administrator review creation,
 * editing, moderation, publication, public retrieval, rating summary,
 * and customer-specific completed-booking eligibility operations.
 *
 * Core business rule:
 *
 * Customer
 *      -> BookingRequest
 *          -> WebsiteService
 *              -> CustomerReview
 *
 * Every customer review represents feedback about an actual
 * completed, review-eligible BookingRequest.
 *
 * Authoritative ownership:
 * - Customer comes from BookingRequest.customerId.
 * - WebsiteService comes from BookingRequest.serviceId.
 * - Verified-customer status is backend-derived.
 * - ContactInquiry does not participate in review ownership.
 *
 * Administrator creation:
 * - bookingRequestId is required.
 * - Customer identity is not supplied independently for review
 *   ownership.
 * - Customer email/phone are not supplied independently.
 * - serviceId is not supplied independently.
 * - verified-customer status is not supplied independently.
 *
 * Customer-first review workflow:
 *
 * 1. Administrator selects/searches for a Customer.
 * 2. The selected customer's customerId is supplied to the
 *    eligible-booking operation.
 * 3. Only completed, review-eligible bookings belonging to that
 *    customer are returned.
 * 4. Administrator selects the exact BookingRequest.
 * 5. BookingRequest remains the authoritative source for both
 *    customer ownership and WebsiteService ownership.
 *
 * Important:
 * customerId in getEligibleBookings(...) is a filtering constraint.
 * It does NOT create a second independent Customer relationship on
 * CustomerReview.
 * ================================================================
 */
public interface CustomerReviewService {

    // =================================================================
    // VERIFIED PUBLIC SUBMISSION
    // =================================================================

    /**
     * Submits a review through a validated customer-review invitation.
     *
     * The invitation supplies the authoritative BookingRequest.
     * Customer and WebsiteService are then resolved from that booking.
     */
    CustomerReview submitVerifiedReview(
            String invitationToken,
            CustomerReview requestedReview,
            UUID customerPhotoMediaId,
            String consentVersion,
            String submissionIpAddress,
            String submissionUserAgent
    );

    // =================================================================
    // ADMIN CREATE
    // =================================================================

    /**
     * Creates an administrator-recorded customer review for one
     * completed, review-eligible booking.
     *
     * Customer and WebsiteService ownership are derived from the
     * selected BookingRequest.
     */
    CustomerReview createReviewByAdministrator(
            CustomerReview requestedReview,
            UUID bookingRequestId,
            UUID customerPhotoMediaId,
            UUID administratorId
    );

    // =================================================================
    // ADMIN UPDATE
    // =================================================================

    /**
     * Updates editable review content.
     *
     * Booking, Customer, customer contact snapshots, WebsiteService,
     * and verified-customer status remain protected.
     */
    CustomerReview updateReview(
            UUID customerReviewId,
            CustomerReview requestedUpdate,
            UUID customerPhotoMediaId,
            UUID administratorId
    );

    // =================================================================
    // GET ONE
    // =================================================================

    CustomerReview getReview(
            UUID customerReviewId
    );

    // =================================================================
    // ELIGIBLE COMPLETED BOOKINGS
    // =================================================================

    /**
     * Returns completed bookings belonging to one selected Customer
     * that may currently receive a customer review.
     *
     * customerId is required.
     *
     * Eligibility requires:
     * - booking.customerId = selected customerId;
     * - status = COMPLETED;
     * - completedAt is populated;
     * - reviewEligible = true;
     * - serviceId is populated;
     * - no CustomerReview already exists for the booking.
     *
     * keyword is optional and may be null. It may further narrow the
     * selected customer's eligible bookings.
     *
     * CustomerReview ownership is still determined from the selected
     * BookingRequest when a review is created.
     */
    Page<CustomerReviewEligibleBookingResponse> getEligibleBookings(
            UUID customerId,
            String keyword,
            Pageable pageable
    );

    // =================================================================
    // ADMIN SEARCH
    // =================================================================

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

    // =================================================================
    // APPROVE
    // =================================================================

    CustomerReview approveReview(
            UUID customerReviewId,
            String moderationNotes,
            UUID administratorId
    );

    // =================================================================
    // REJECT
    // =================================================================

    CustomerReview rejectReview(
            UUID customerReviewId,
            String rejectionReason,
            String moderationNotes,
            UUID administratorId
    );

    // =================================================================
    // SPAM
    // =================================================================

    CustomerReview markReviewAsSpam(
            UUID customerReviewId,
            BigDecimal spamScore,
            String moderationNotes,
            UUID administratorId
    );

    // =================================================================
    // PUBLISH
    // =================================================================

    CustomerReview publishReview(
            UUID customerReviewId,
            boolean featured,
            UUID administratorId
    );

    // =================================================================
    // UNPUBLISH
    // =================================================================

    CustomerReview unpublishReview(
            UUID customerReviewId,
            UUID administratorId
    );

    // =================================================================
    // FEATURED STATUS
    // =================================================================

    CustomerReview updateFeaturedStatus(
            UUID customerReviewId,
            boolean featured,
            UUID administratorId
    );

    // =================================================================
    // HIDE
    // =================================================================

    CustomerReview hideReview(
            UUID customerReviewId,
            String moderationNotes,
            UUID administratorId
    );

    // =================================================================
    // ARCHIVE
    // =================================================================

    CustomerReview archiveReview(
            UUID customerReviewId,
            UUID administratorId
    );

    // =================================================================
    // ADMIN RESPONSE
    // =================================================================

    CustomerReview addAdminResponse(
            UUID customerReviewId,
            String response,
            UUID administratorId
    );

    CustomerReview removeAdminResponse(
            UUID customerReviewId,
            UUID administratorId
    );

    // =================================================================
    // PUBLIC REVIEWS
    // =================================================================

    Page<CustomerReview> getPublicReviews(
            Pageable pageable
    );

    List<CustomerReview> getFeaturedPublicReviews();

    Page<CustomerReview> getPublicReviewsByServiceSlug(
            String serviceSlug,
            Pageable pageable
    );

    // =================================================================
    // PUBLIC RATING SUMMARY
    // =================================================================

    CustomerReviewRatingSummaryResponse
    getPublicRatingSummary();
}