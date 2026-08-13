package romelt_techcare.backend.service.implement;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import romelt_techcare.backend.dto.CustomerReviewEligibleBookingResponse;
import romelt_techcare.backend.dto.CustomerReviewRatingSummaryResponse;
import romelt_techcare.backend.entity.AdminUser;
import romelt_techcare.backend.entity.BookingRequest;
import romelt_techcare.backend.entity.Customer;
import romelt_techcare.backend.entity.CustomerReview;
import romelt_techcare.backend.entity.CustomerReviewInvitation;
import romelt_techcare.backend.entity.WebsiteMediaAsset;
import romelt_techcare.backend.entity.WebsiteService;
import romelt_techcare.backend.enums.BookingRequestStatus;
import romelt_techcare.backend.enums.CustomerReviewDisplayPreference;
import romelt_techcare.backend.enums.CustomerReviewModerationStatus;
import romelt_techcare.backend.enums.CustomerReviewSource;
import romelt_techcare.backend.enums.WebsiteContentAuditAction;
import romelt_techcare.backend.enums.WebsiteContentAuditResourceType;
import romelt_techcare.backend.repository.AdminUserRepository;
import romelt_techcare.backend.repository.BookingRequestRepository;
import romelt_techcare.backend.repository.CustomerRepository;
import romelt_techcare.backend.repository.CustomerReviewEligibleBookingRepository;
import romelt_techcare.backend.repository.CustomerReviewRepository;
import romelt_techcare.backend.repository.WebsiteMediaAssetRepository;
import romelt_techcare.backend.repository.WebsiteServiceRepository;
import romelt_techcare.backend.service.CustomerReviewInvitationService;
import romelt_techcare.backend.service.CustomerReviewService;
import romelt_techcare.backend.service.WebsiteContentAuditLogService;
import romelt_techcare.backend.service.WebsiteContentAuditSnapshotService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — CUSTOMER REVIEW SERVICE IMPLEMENTATION
 * ================================================================
 *
 * Purpose:
 * Implements service-based customer-review submission, administrator
 * review entry, moderation, publication, public display, rating
 * aggregation, and immutable audit logging.
 *
 * Core business rule:
 * A Romelt TechCare customer review represents feedback about an
 * actual completed, review-eligible BookingRequest.
 *
 * Customer identity and WebsiteService identity are derived from the
 * completed booking and reusable Customer record. Browser-supplied
 * contact or service identity is never authoritative.
 *
 * Verified-submission transaction:
 * 1. Validates and locks the invitation token.
 * 2. Ensures no prior review exists for the invitation or booking.
 * 3. Creates the pending review.
 * 4. Consumes the invitation.
 * 5. Records the customer-review audit event.
 *
 * Security:
 * - Public submissions cannot set moderation or publication fields.
 * - Customer email and booking identity are derived from the verified
 *   invitation.
 * - Audit snapshots exclude customer email, phone, IP addresses,
 *   user agents, consent IP addresses, and full review text.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerReviewServiceImplementation
        implements CustomerReviewService {

    private final CustomerReviewRepository
            customerReviewRepository;

    private final CustomerReviewEligibleBookingRepository
            customerReviewEligibleBookingRepository;

    private final CustomerReviewInvitationService
            customerReviewInvitationService;

    private final BookingRequestRepository
            bookingRequestRepository;

    private final CustomerRepository
            customerRepository;

    private final WebsiteServiceRepository
            websiteServiceRepository;

    private final WebsiteMediaAssetRepository
            websiteMediaAssetRepository;

    private final AdminUserRepository
            adminUserRepository;

    private final WebsiteContentAuditLogService
            websiteContentAuditLogService;

    private final WebsiteContentAuditSnapshotService
            websiteContentAuditSnapshotService;

    // =================================================================
    // VERIFIED PUBLIC SUBMISSION
    // =================================================================

    @Override
    @Transactional
    public CustomerReview submitVerifiedReview(
            String invitationToken,
            CustomerReview requestedReview,
            UUID customerPhotoMediaId,
            String consentVersion,
            String submissionIpAddress,
            String submissionUserAgent
    ) {
        if (requestedReview == null) {
            throw badRequest(
                    "Customer review information is required."
            );
        }

        validateReviewContent(requestedReview);

        if (
                !Boolean.TRUE.equals(
                        requestedReview
                                .getCustomerConsentConfirmed()
                )
        ) {
            throw badRequest(
                    "Customer consent must be confirmed."
            );
        }

        String normalizedConsentVersion =
                normalizeRequired(
                        consentVersion,
                        "Consent version"
                );

        CustomerReviewInvitation invitation =
                customerReviewInvitationService
                        .validatePublicToken(invitationToken);

        BookingRequest bookingRequest =
                invitation.getBookingRequest();

        if (bookingRequest == null) {
            throw conflict(
                    "The review invitation is not associated with a booking."
            );
        }

        requireReviewEligibleCompletedBooking(
                bookingRequest
        );

        if (
                customerReviewRepository
                        .existsByReviewInvitation_ReviewInvitationId(
                                invitation.getReviewInvitationId()
                        )
        ) {
            throw conflict(
                    "A review has already been submitted with this invitation."
            );
        }

        if (
                customerReviewRepository
                        .existsByBookingRequest_BookingRequestId(
                                bookingRequest
                                        .getBookingRequestId()
                        )
        ) {
            throw conflict(
                    "A review has already been submitted for this booking."
            );
        }

        Customer customer =
                getRequiredBookingCustomer(
                        bookingRequest
                );

        WebsiteService websiteService =
                getRequiredBookingWebsiteService(
                        bookingRequest
                );

        WebsiteMediaAsset customerPhoto =
                getOptionalPublicMediaAsset(
                        customerPhotoMediaId
                );

        String displayName =
                resolveDisplayNameFromCustomer(
                        requestedReview,
                        customer
                );

        CustomerReview review =
                CustomerReview.builder()
                        .reviewInvitation(invitation)
                        .bookingRequest(bookingRequest)
                        .websiteService(websiteService)
                        .reviewerDisplayName(displayName)
                        .reviewerDisplayPreference(
                                requestedReview
                                        .getReviewerDisplayPreference()
                        )
                        .reviewerEmail(
                                normalizeOptional(
                                        customer.getPrimaryEmail()
                                )
                        )
                        .reviewerPhone(
                                normalizeOptional(
                                        customer.getPrimaryPhone()
                                )
                        )
                        .reviewTitle(
                                normalizeOptional(
                                        requestedReview
                                                .getReviewTitle()
                                )
                        )
                        .reviewText(
                                normalizeRequired(
                                        requestedReview.getReviewText(),
                                        "Review text"
                                )
                        )
                        .rating(
                                requestedReview.getRating()
                        )
                        .reviewSource(
                                CustomerReviewSource
                                        .BOOKING_FOLLOW_UP
                        )
                        .customerPhotoMedia(
                                customerPhoto
                        )
                        .isVerifiedCustomer(true)
                        .customerConsentConfirmed(true)
                        .customerConsentConfirmedAt(
                                Instant.now()
                        )
                        .customerConsentVersion(
                                normalizedConsentVersion
                        )
                        .customerConsentIpAddress(
                                normalizeOptional(
                                        submissionIpAddress
                                )
                        )
                        .moderationStatus(
                                CustomerReviewModerationStatus.PENDING
                        )
                        .isPublic(false)
                        .isFeatured(false)
                        .submissionIpAddress(
                                normalizeOptional(
                                        submissionIpAddress
                                )
                        )
                        .submissionUserAgent(
                                truncate(
                                        submissionUserAgent,
                                        500
                                )
                        )
                        .isSpam(false)
                        .submittedAt(
                                Instant.now()
                        )
                        .build();

        CustomerReview savedReview =
                saveReview(
                        review,
                        "Unable to submit the review. A review may "
                                + "already exist for this booking or invitation."
                );

        customerReviewInvitationService
                .consumeInvitation(
                        invitationToken
                );

        websiteContentAuditLogService.recordAudit(
                null,
                WebsiteContentAuditAction.CREATE,
                WebsiteContentAuditResourceType.CUSTOMER_REVIEW,
                savedReview.getCustomerReviewId(),
                createReviewResourceName(
                        savedReview
                ),
                null,
                createReviewSnapshot(
                        savedReview
                ),
                "Verified customer review submitted and queued for moderation.",
                null
        );

        return getReview(
                savedReview.getCustomerReviewId()
        );
    }

    // =================================================================
    // ADMIN CREATE
    // =================================================================

    @Override
    @Transactional
    public CustomerReview createReviewByAdministrator(
            CustomerReview requestedReview,
            UUID bookingRequestId,
            UUID customerPhotoMediaId,
            UUID administratorId
    ) {
        if (requestedReview == null) {
            throw badRequest(
                    "Customer review information is required."
            );
        }

        validateReviewContent(
                requestedReview
        );

        if (
                requestedReview.getReviewSource()
                        == null
        ) {
            throw badRequest(
                    "Review source is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(
                        administratorId
                );

        BookingRequest bookingRequest =
                getRequiredBookingRequest(
                        bookingRequestId
                );

        requireReviewEligibleCompletedBooking(
                bookingRequest
        );

        if (
                customerReviewRepository
                        .existsByBookingRequest_BookingRequestId(
                                bookingRequest.getBookingRequestId()
                        )
        ) {
            throw conflict(
                    "A review already exists for this completed service."
            );
        }

        Customer customer =
                getRequiredBookingCustomer(
                        bookingRequest
                );

        WebsiteService websiteService =
                getRequiredBookingWebsiteService(
                        bookingRequest
                );

        WebsiteMediaAsset customerPhoto =
                getOptionalPublicMediaAsset(
                        customerPhotoMediaId
                );

        boolean consentConfirmed =
                Boolean.TRUE.equals(
                        requestedReview
                                .getCustomerConsentConfirmed()
                );

        String displayName =
                resolveDisplayNameFromCustomer(
                        requestedReview,
                        customer
                );

        CustomerReview review =
                CustomerReview.builder()
                        .bookingRequest(
                                bookingRequest
                        )
                        .websiteService(
                                websiteService
                        )
                        .reviewerDisplayName(
                                displayName
                        )
                        .reviewerDisplayPreference(
                                requestedReview
                                        .getReviewerDisplayPreference()
                        )
                        .reviewerEmail(
                                normalizeOptional(
                                        customer.getPrimaryEmail()
                                )
                        )
                        .reviewerPhone(
                                normalizeOptional(
                                        customer.getPrimaryPhone()
                                )
                        )
                        .reviewTitle(
                                normalizeOptional(
                                        requestedReview
                                                .getReviewTitle()
                                )
                        )
                        .reviewText(
                                normalizeRequired(
                                        requestedReview
                                                .getReviewText(),
                                        "Review text"
                                )
                        )
                        .rating(
                                requestedReview.getRating()
                        )
                        .reviewSource(
                                requestedReview.getReviewSource()
                        )
                        .externalSourceUrl(
                                normalizeOptional(
                                        requestedReview
                                                .getExternalSourceUrl()
                                )
                        )
                        .customerPhotoMedia(
                                customerPhoto
                        )
                        .isVerifiedCustomer(true)
                        .customerConsentConfirmed(
                                consentConfirmed
                        )
                        .customerConsentConfirmedAt(
                                consentConfirmed
                                        ? Instant.now()
                                        : null
                        )
                        .customerConsentVersion(
                                consentConfirmed
                                        ? normalizeRequired(
                                        requestedReview
                                                .getCustomerConsentVersion(),
                                        "Consent version"
                                )
                                        : null
                        )
                        .moderationStatus(
                                CustomerReviewModerationStatus.PENDING
                        )
                        .isPublic(false)
                        .isFeatured(false)
                        .isSpam(false)
                        .submittedAt(
                                Instant.now()
                        )
                        .createdByAdminUser(
                                administrator
                        )
                        .updatedByAdminUser(
                                administrator
                        )
                        .build();

        CustomerReview savedReview =
                saveReview(
                        review,
                        "Unable to create the customer review."
                );

        websiteContentAuditLogService.recordAudit(
                administratorId,
                WebsiteContentAuditAction.CREATE,
                WebsiteContentAuditResourceType.CUSTOMER_REVIEW,
                savedReview.getCustomerReviewId(),
                createReviewResourceName(
                        savedReview
                ),
                null,
                createReviewSnapshot(
                        savedReview
                ),
                "Administrator created a review for a completed customer service.",
                null
        );

        return getReview(
                savedReview.getCustomerReviewId()
        );
    }

    // =================================================================
    // UPDATE
    // =================================================================

    @Override
    @Transactional
    public CustomerReview updateReview(
            UUID customerReviewId,
            CustomerReview requestedUpdate,
            UUID customerPhotoMediaId,
            UUID administratorId
    ) {
        if (requestedUpdate == null) {
            throw badRequest(
                    "Updated customer review information is required."
            );
        }

        validateReviewContent(
                requestedUpdate
        );

        if (
                requestedUpdate.getReviewSource()
                        == null
        ) {
            throw badRequest(
                    "Review source is required."
            );
        }

        AdminUser administrator =
                getRequiredAdministrator(
                        administratorId
                );

        CustomerReview existingReview =
                getReviewForUpdate(
                        customerReviewId
                );

        BookingRequest bookingRequest =
                existingReview.getBookingRequest();

        if (bookingRequest == null) {
            throw conflict(
                    "Customer review is not associated with a completed booking."
            );
        }

        requireReviewEligibleCompletedBooking(
                bookingRequest
        );

        /*
         * Defensive integrity check:
         * the service stored on the review must still be the same
         * service identified by its authoritative completed booking.
         */
        requireReviewServiceMatchesBooking(
                existingReview,
                bookingRequest
        );

        JsonNode beforeSnapshot =
                createReviewSnapshot(
                        existingReview
                );

        WebsiteMediaAsset customerPhoto =
                getOptionalPublicMediaAsset(
                        customerPhotoMediaId
                );

        String displayName =
                resolveUpdatedDisplayName(
                        requestedUpdate,
                        existingReview
                );

        try {
            existingReview.updateContent(
                    displayName,

                    requestedUpdate
                            .getReviewerDisplayPreference(),

                    normalizeOptional(
                            requestedUpdate
                                    .getReviewTitle()
                    ),

                    normalizeRequired(
                            requestedUpdate
                                    .getReviewText(),
                            "Review text"
                    ),

                    requestedUpdate.getRating(),

                    requestedUpdate.getReviewSource(),

                    normalizeOptional(
                            requestedUpdate
                                    .getExternalSourceUrl()
                    ),

                    customerPhoto,

                    administrator
            );

        } catch (
                IllegalStateException exception
        ) {
            throw conflict(
                    exception.getMessage()
            );
        }

        CustomerReview savedReview =
                customerReviewRepository
                        .saveAndFlush(
                                existingReview
                        );

        recordReviewAudit(
                administratorId,
                WebsiteContentAuditAction.UPDATE,
                savedReview,
                beforeSnapshot,
                "Customer review content updated."
        );

        return getReview(
                savedReview.getCustomerReviewId()
        );
    }

    // =================================================================
    // GET ONE
    // =================================================================

    @Override
    public CustomerReview getReview(
            UUID customerReviewId
    ) {
        requireIdentifier(
                customerReviewId,
                "Customer review ID"
        );

        return customerReviewRepository
                .findByCustomerReviewId(
                        customerReviewId
                )
                .orElseThrow(
                        () -> notFound(
                                "Customer review was not found."
                        )
                );
    }

    // =================================================================
// ELIGIBLE COMPLETED BOOKINGS
// =================================================================

    @Override
    public Page<CustomerReviewEligibleBookingResponse> getEligibleBookings(
            UUID customerId,
            String keyword,
            Pageable pageable
    ) {
        requireIdentifier(
                customerId,
                "Customer ID"
        );

        requirePageable(
                pageable
        );

        Customer customer =
                customerRepository
                        .findActiveRecordById(
                                customerId
                        )
                        .orElseThrow(
                                () -> notFound(
                                        "Customer was not found."
                                )
                        );

        return customerReviewEligibleBookingRepository
                .findEligibleBookingsByCustomer(
                        customer.getCustomerId(),
                        normalizeOptional(
                                keyword
                        ),
                        pageable
                )
                .map(
                        this::toEligibleBookingResponse
                );
    }

    private CustomerReviewEligibleBookingResponse toEligibleBookingResponse(
            BookingRequest bookingRequest
    ) {
        requireReviewEligibleCompletedBooking(
                bookingRequest
        );

        Customer customer =
                getRequiredBookingCustomer(
                        bookingRequest
                );

        WebsiteService websiteService =
                getRequiredBookingWebsiteService(
                        bookingRequest
                );

        return new CustomerReviewEligibleBookingResponse(
                bookingRequest.getBookingRequestId(),
                bookingRequest.getReferenceNumber(),
                bookingRequest.getCustomerId(),
                customer.getCustomerNumber(),
                customer.getDisplayName(),
                customer.getPrimaryEmail(),
                customer.getPrimaryPhone(),
                websiteService.getServiceId(),
                websiteService.getServiceCode(),
                websiteService.getServiceSlug(),
                bookingRequest.getServiceType(),
                bookingRequest.getServiceMethod(),
                bookingRequest.getCompletedAt(),
                bookingRequest.getCompletionSummary()
        );
    }

    // =================================================================
    // SEARCH
    // =================================================================

    @Override
    public Page<CustomerReview> searchReviews(
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
    ) {
        requirePageable(
                pageable
        );

        if (
                rating != null
                        && (
                        rating < 1
                                || rating > 5
                )
        ) {
            throw badRequest(
                    "Rating must be between 1 and 5."
            );
        }

        return customerReviewRepository
                .searchReviews(
                        normalizeOptional(
                                keyword
                        ),
                        moderationStatus,
                        reviewSource,
                        rating,
                        serviceId,
                        isVerifiedCustomer,
                        isPublic,
                        isFeatured,
                        isSpam,
                        pageable
                );
    }

    // =================================================================
    // APPROVE
    // =================================================================

    @Override
    @Transactional
    public CustomerReview approveReview(
            UUID customerReviewId,
            String moderationNotes,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(
                        administratorId
                );

        CustomerReview review =
                getReviewForUpdate(
                        customerReviewId
                );

        JsonNode beforeSnapshot =
                createReviewSnapshot(
                        review
                );

        try {
            review.approve(
                    normalizeOptional(
                            moderationNotes
                    ),
                    administrator
            );
        } catch (
                IllegalStateException exception
        ) {
            throw conflict(
                    exception.getMessage()
            );
        }

        CustomerReview savedReview =
                customerReviewRepository
                        .saveAndFlush(
                                review
                        );

        recordReviewAudit(
                administratorId,
                WebsiteContentAuditAction.APPROVE,
                savedReview,
                beforeSnapshot,
                "Customer review approved."
        );

        return savedReview;
    }

    // =================================================================
    // REJECT
    // =================================================================

    @Override
    @Transactional
    public CustomerReview rejectReview(
            UUID customerReviewId,
            String rejectionReason,
            String moderationNotes,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(
                        administratorId
                );

        CustomerReview review =
                getReviewForUpdate(
                        customerReviewId
                );

        JsonNode beforeSnapshot =
                createReviewSnapshot(
                        review
                );

        try {
            review.reject(
                    normalizeRequired(
                            rejectionReason,
                            "Rejection reason"
                    ),
                    normalizeOptional(
                            moderationNotes
                    ),
                    administrator
            );
        } catch (
                IllegalStateException exception
        ) {
            throw conflict(
                    exception.getMessage()
            );
        }

        CustomerReview savedReview =
                customerReviewRepository
                        .saveAndFlush(
                                review
                        );

        recordReviewAudit(
                administratorId,
                WebsiteContentAuditAction.REJECT,
                savedReview,
                beforeSnapshot,
                "Customer review rejected."
        );

        return savedReview;
    }

    // =================================================================
    // SPAM
    // =================================================================

    @Override
    @Transactional
    public CustomerReview markReviewAsSpam(
            UUID customerReviewId,
            BigDecimal spamScore,
            String moderationNotes,
            UUID administratorId
    ) {
        validateSpamScore(
                spamScore
        );

        AdminUser administrator =
                getRequiredAdministrator(
                        administratorId
                );

        CustomerReview review =
                getReviewForUpdate(
                        customerReviewId
                );

        JsonNode beforeSnapshot =
                createReviewSnapshot(
                        review
                );

        try {
            review.markSpam(
                    spamScore,
                    normalizeOptional(
                            moderationNotes
                    ),
                    administrator
            );
        } catch (
                IllegalStateException exception
        ) {
            throw conflict(
                    exception.getMessage()
            );
        }

        CustomerReview savedReview =
                customerReviewRepository
                        .saveAndFlush(
                                review
                        );

        recordReviewAudit(
                administratorId,
                WebsiteContentAuditAction.MARK_SPAM,
                savedReview,
                beforeSnapshot,
                "Customer review classified as spam."
        );

        return savedReview;
    }

    // =================================================================
    // PUBLISH
    // =================================================================

    @Override
    @Transactional
    public CustomerReview publishReview(
            UUID customerReviewId,
            boolean featured,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(
                        administratorId
                );

        CustomerReview review =
                getReviewForUpdate(
                        customerReviewId
                );

        JsonNode beforeSnapshot =
                createReviewSnapshot(
                        review
                );

        try {
            review.publish(
                    featured,
                    administrator
            );
        } catch (
                IllegalStateException exception
        ) {
            throw conflict(
                    exception.getMessage()
            );
        }

        CustomerReview savedReview =
                customerReviewRepository
                        .saveAndFlush(
                                review
                        );

        recordReviewAudit(
                administratorId,
                WebsiteContentAuditAction.PUBLISH,
                savedReview,
                beforeSnapshot,
                featured
                        ? "Customer review published and featured."
                        : "Customer review published."
        );

        return savedReview;
    }

    // =================================================================
    // UNPUBLISH
    // =================================================================

    @Override
    @Transactional
    public CustomerReview unpublishReview(
            UUID customerReviewId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(
                        administratorId
                );

        CustomerReview review =
                getReviewForUpdate(
                        customerReviewId
                );

        JsonNode beforeSnapshot =
                createReviewSnapshot(
                        review
                );

        review.unpublish(
                administrator
        );

        CustomerReview savedReview =
                customerReviewRepository
                        .saveAndFlush(
                                review
                        );

        recordReviewAudit(
                administratorId,
                WebsiteContentAuditAction.UNPUBLISH,
                savedReview,
                beforeSnapshot,
                "Customer review removed from public display."
        );

        return savedReview;
    }

    // =================================================================
    // FEATURED STATUS
    // =================================================================

    @Override
    @Transactional
    public CustomerReview updateFeaturedStatus(
            UUID customerReviewId,
            boolean featured,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(
                        administratorId
                );

        CustomerReview review =
                getReviewForUpdate(
                        customerReviewId
                );

        JsonNode beforeSnapshot =
                createReviewSnapshot(
                        review
                );

        try {
            review.setFeatured(
                    featured,
                    administrator
            );
        } catch (
                IllegalStateException exception
        ) {
            throw conflict(
                    exception.getMessage()
            );
        }

        CustomerReview savedReview =
                customerReviewRepository
                        .saveAndFlush(
                                review
                        );

        recordReviewAudit(
                administratorId,
                featured
                        ? WebsiteContentAuditAction.FEATURE
                        : WebsiteContentAuditAction.UNFEATURE,
                savedReview,
                beforeSnapshot,
                featured
                        ? "Customer review featured."
                        : "Customer review removed from featured placement."
        );

        return savedReview;
    }

    // =================================================================
    // HIDE
    // =================================================================

    @Override
    @Transactional
    public CustomerReview hideReview(
            UUID customerReviewId,
            String moderationNotes,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(
                        administratorId
                );

        CustomerReview review =
                getReviewForUpdate(
                        customerReviewId
                );

        JsonNode beforeSnapshot =
                createReviewSnapshot(
                        review
                );

        try {
            review.hide(
                    normalizeOptional(
                            moderationNotes
                    ),
                    administrator
            );
        } catch (
                IllegalStateException exception
        ) {
            throw conflict(
                    exception.getMessage()
            );
        }

        CustomerReview savedReview =
                customerReviewRepository
                        .saveAndFlush(
                                review
                        );

        recordReviewAudit(
                administratorId,
                WebsiteContentAuditAction.HIDE,
                savedReview,
                beforeSnapshot,
                "Customer review hidden."
        );

        return savedReview;
    }

    // =================================================================
    // ARCHIVE
    // =================================================================

    @Override
    @Transactional
    public CustomerReview archiveReview(
            UUID customerReviewId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(
                        administratorId
                );

        CustomerReview review =
                getReviewForUpdate(
                        customerReviewId
                );

        JsonNode beforeSnapshot =
                createReviewSnapshot(
                        review
                );

        review.archive(
                administrator
        );

        CustomerReview savedReview =
                customerReviewRepository
                        .saveAndFlush(
                                review
                        );

        recordReviewAudit(
                administratorId,
                WebsiteContentAuditAction.ARCHIVE,
                savedReview,
                beforeSnapshot,
                "Customer review archived."
        );

        return savedReview;
    }

    // =================================================================
    // ADMIN RESPONSE
    // =================================================================

    @Override
    @Transactional
    public CustomerReview addAdminResponse(
            UUID customerReviewId,
            String response,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(
                        administratorId
                );

        CustomerReview review =
                getReviewForUpdate(
                        customerReviewId
                );

        JsonNode beforeSnapshot =
                createReviewSnapshot(
                        review
                );

        review.addAdminResponse(
                normalizeRequired(
                        response,
                        "Administrator response"
                ),
                administrator
        );

        CustomerReview savedReview =
                customerReviewRepository
                        .saveAndFlush(
                                review
                        );

        recordReviewAudit(
                administratorId,
                WebsiteContentAuditAction.RESPOND,
                savedReview,
                beforeSnapshot,
                "Administrator response added to customer review."
        );

        return savedReview;
    }

    @Override
    @Transactional
    public CustomerReview removeAdminResponse(
            UUID customerReviewId,
            UUID administratorId
    ) {
        AdminUser administrator =
                getRequiredAdministrator(
                        administratorId
                );

        CustomerReview review =
                getReviewForUpdate(
                        customerReviewId
                );

        JsonNode beforeSnapshot =
                createReviewSnapshot(
                        review
                );

        review.removeAdminResponse(
                administrator
        );

        CustomerReview savedReview =
                customerReviewRepository
                        .saveAndFlush(
                                review
                        );

        recordReviewAudit(
                administratorId,
                WebsiteContentAuditAction.REMOVE_RESPONSE,
                savedReview,
                beforeSnapshot,
                "Administrator response removed from customer review."
        );

        return savedReview;
    }

    // =================================================================
    // PUBLIC REVIEWS
    // =================================================================

    @Override
    public Page<CustomerReview> getPublicReviews(
            Pageable pageable
    ) {
        requirePageable(
                pageable
        );

        return customerReviewRepository
                .findPublicReviews(
                        pageable
                );
    }

    @Override
    public List<CustomerReview>
    getFeaturedPublicReviews() {
        return customerReviewRepository
                .findFeaturedPublicReviews();
    }

    @Override
    public Page<CustomerReview>
    getPublicReviewsByServiceSlug(
            String serviceSlug,
            Pageable pageable
    ) {
        requirePageable(
                pageable
        );

        String normalizedSlug =
                normalizeSlug(
                        serviceSlug
                );

        return customerReviewRepository
                .findPublicReviewsByServiceSlug(
                        normalizedSlug,
                        pageable
                );
    }

    // =================================================================
    // PUBLIC RATING SUMMARY
    // =================================================================

    @Override
    public CustomerReviewRatingSummaryResponse
    getPublicRatingSummary() {

        long totalReviews =
                customerReviewRepository
                        .countPublicReviews();

        Double average =
                customerReviewRepository
                        .calculateAveragePublicRating();

        double roundedAverage =
                average == null
                        ? 0.0
                        : BigDecimal
                        .valueOf(
                                average
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                        .doubleValue();

        return new CustomerReviewRatingSummaryResponse(
                totalReviews,

                roundedAverage,

                customerReviewRepository
                        .countPublicReviewsByRating(
                                (short) 5
                        ),

                customerReviewRepository
                        .countPublicReviewsByRating(
                                (short) 4
                        ),

                customerReviewRepository
                        .countPublicReviewsByRating(
                                (short) 3
                        ),

                customerReviewRepository
                        .countPublicReviewsByRating(
                                (short) 2
                        ),

                customerReviewRepository
                        .countPublicReviewsByRating(
                                (short) 1
                        )
        );
    }

    // =================================================================
    // REVIEW AUDIT
    // =================================================================

    private void recordReviewAudit(
            UUID administratorId,
            WebsiteContentAuditAction action,
            CustomerReview review,
            JsonNode beforeSnapshot,
            String summary
    ) {
        websiteContentAuditLogService.recordAudit(
                administratorId,
                action,
                WebsiteContentAuditResourceType.CUSTOMER_REVIEW,
                review.getCustomerReviewId(),
                createReviewResourceName(
                        review
                ),
                beforeSnapshot,
                createReviewSnapshot(
                        review
                ),
                summary,
                null
        );
    }

    /**
     * Builds a deliberately non-sensitive audit snapshot.
     *
     * Excluded:
     * - reviewer email;
     * - reviewer phone;
     * - review text;
     * - submission IP;
     * - consent IP;
     * - user agent.
     */
    private JsonNode createReviewSnapshot(
            CustomerReview review
    ) {
        Map<String, Object> fields =
                new LinkedHashMap<>();

        fields.put(
                "customerReviewId",
                review.getCustomerReviewId()
        );

        fields.put(
                "reviewInvitationId",
                review.getReviewInvitation()
                        == null
                        ? null
                        : review
                        .getReviewInvitation()
                        .getReviewInvitationId()
        );

        fields.put(
                "bookingRequestId",
                review.getBookingRequest()
                        == null
                        ? null
                        : review
                        .getBookingRequest()
                        .getBookingRequestId()
        );

        fields.put(
                "serviceId",
                review.getWebsiteService()
                        == null
                        ? null
                        : review
                        .getWebsiteService()
                        .getServiceId()
        );

        fields.put(
                "reviewerDisplayName",
                review.getReviewerDisplayName()
        );

        fields.put(
                "reviewerDisplayPreference",
                review.getReviewerDisplayPreference()
        );

        fields.put(
                "reviewTitle",
                review.getReviewTitle()
        );

        fields.put(
                "rating",
                review.getRating()
        );

        fields.put(
                "reviewSource",
                review.getReviewSource()
        );

        fields.put(
                "isVerifiedCustomer",
                review.getIsVerifiedCustomer()
        );

        fields.put(
                "customerConsentConfirmed",
                review.getCustomerConsentConfirmed()
        );

        fields.put(
                "moderationStatus",
                review.getModerationStatus()
        );

        fields.put(
                "isPublic",
                review.getIsPublic()
        );

        fields.put(
                "isFeatured",
                review.getIsFeatured()
        );

        fields.put(
                "isSpam",
                review.getIsSpam()
        );

        fields.put(
                "spamScore",
                review.getSpamScore()
        );

        fields.put(
                "hasAdminResponse",
                normalizeOptional(
                        review.getAdminResponse()
                ) != null
        );

        fields.put(
                "submittedAt",
                review.getSubmittedAt()
        );

        fields.put(
                "moderatedAt",
                review.getModeratedAt()
        );

        fields.put(
                "publishedAt",
                review.getPublishedAt()
        );

        fields.put(
                "hiddenAt",
                review.getHiddenAt()
        );

        fields.put(
                "archivedAt",
                review.getArchivedAt()
        );

        return websiteContentAuditSnapshotService
                .createSnapshot(
                        fields
                );
    }

    private String createReviewResourceName(
            CustomerReview review
    ) {
        String title =
                normalizeOptional(
                        review.getReviewTitle()
                );

        if (title != null) {
            return title;
        }

        String displayName =
                normalizeOptional(
                        review.getReviewerDisplayName()
                );

        if (displayName != null) {
            return "Review by "
                    + displayName;
        }

        return "Customer Review";
    }

    // =================================================================
    // REVIEW LOOKUPS
    // =================================================================

    private CustomerReview getReviewForUpdate(
            UUID customerReviewId
    ) {
        requireIdentifier(
                customerReviewId,
                "Customer review ID"
        );

        return customerReviewRepository
                .findByIdForUpdate(
                        customerReviewId
                )
                .orElseThrow(
                        () -> notFound(
                                "Customer review was not found."
                        )
                );
    }

    private BookingRequest getRequiredBookingRequest(
            UUID bookingRequestId
    ) {
        requireIdentifier(
                bookingRequestId,
                "Booking request ID"
        );

        return bookingRequestRepository
                .findById(
                        bookingRequestId
                )
                .orElseThrow(
                        () -> notFound(
                                "Booking request was not found."
                        )
                );
    }

    // =================================================================
    // COMPLETED BOOKING VALIDATION
    // =================================================================

    private void requireReviewEligibleCompletedBooking(
            BookingRequest bookingRequest
    ) {
        if (bookingRequest == null) {
            throw badRequest(
                    "Booking request is required."
            );
        }

        if (
                bookingRequest.getStatus()
                        != BookingRequestStatus.COMPLETED
        ) {
            throw conflict(
                    "Only completed services can receive customer reviews."
            );
        }

        if (
                bookingRequest.getCompletedAt()
                        == null
        ) {
            throw conflict(
                    "The booking does not contain a service completion record."
            );
        }

        if (
                !bookingRequest.isReviewEligible()
        ) {
            throw conflict(
                    "This completed service is not eligible for customer review."
            );
        }

        if (
                bookingRequest.getCustomerId()
                        == null
        ) {
            throw conflict(
                    "The completed booking is not linked to a customer."
            );
        }

        if (
                bookingRequest.getServiceId()
                        == null
        ) {
            throw conflict(
                    "The completed booking is not linked to a service."
            );
        }
    }

    // =================================================================
    // CUSTOMER RESOLUTION
    // =================================================================

    private Customer getRequiredBookingCustomer(
            BookingRequest bookingRequest
    ) {
        UUID customerId =
                bookingRequest.getCustomerId();

        if (customerId == null) {
            throw conflict(
                    "The completed booking is not linked to a customer."
            );
        }

        return customerRepository
                .findActiveRecordById(
                        customerId
                )
                .orElseThrow(
                        () -> notFound(
                                "The customer linked to this booking was not found."
                        )
                );
    }

    // =================================================================
    // SERVICE RESOLUTION
    // =================================================================

    /**
     * Resolves the service exclusively from the completed booking.
     *
     * There is intentionally no optional/browser-selected service
     * resolver in this service anymore.
     */
    private WebsiteService getRequiredBookingWebsiteService(
            BookingRequest bookingRequest
    ) {
        UUID serviceId =
                bookingRequest.getServiceId();

        if (serviceId == null) {
            throw conflict(
                    "The completed booking is not linked to a service."
            );
        }

        WebsiteService service =
                websiteServiceRepository
                        .findById(
                                serviceId
                        )
                        .orElseThrow(
                                () -> notFound(
                                        "The service linked to this booking was not found."
                                )
                        );

        if (
                service.isDeleted()
        ) {
            throw conflict(
                    "The service linked to this booking has been deleted."
            );
        }

        return service;
    }

    /**
     * Ensures an existing review still points to the exact WebsiteService
     * identified by the authoritative completed booking.
     *
     * This is a defensive data-integrity check for existing records.
     */
    private void requireReviewServiceMatchesBooking(
            CustomerReview review,
            BookingRequest bookingRequest
    ) {
        if (review == null) {
            throw badRequest(
                    "Customer review is required."
            );
        }

        if (bookingRequest == null) {
            throw badRequest(
                    "Booking request is required."
            );
        }

        WebsiteService reviewService =
                review.getWebsiteService();

        if (reviewService == null) {
            throw conflict(
                    "Customer review is not associated with the service performed."
            );
        }

        UUID bookingServiceId =
                bookingRequest.getServiceId();

        UUID reviewServiceId =
                reviewService.getServiceId();

        if (
                bookingServiceId == null
                        || reviewServiceId == null
                        || !bookingServiceId.equals(
                        reviewServiceId
                )
        ) {
            throw conflict(
                    "Customer review service does not match the completed booking service."
            );
        }
    }

    // =================================================================
    // REVIEW PHOTO
    // =================================================================

    private WebsiteMediaAsset getOptionalPublicMediaAsset(
            UUID mediaAssetId
    ) {
        if (mediaAssetId == null) {
            return null;
        }

        WebsiteMediaAsset mediaAsset =
                websiteMediaAssetRepository
                        .findById(
                                mediaAssetId
                        )
                        .orElseThrow(
                                () -> notFound(
                                        "Website media asset was not found."
                                )
                        );

        if (
                !Boolean.TRUE.equals(
                        mediaAsset.getIsPublic()
                )
        ) {
            throw conflict(
                    "Customer review photos must be public media assets."
            );
        }

        return mediaAsset;
    }

    // =================================================================
    // REVIEW CONTENT VALIDATION
    // =================================================================

    private void validateReviewContent(
            CustomerReview review
    ) {
        if (
                review.getRating() == null
                        || review.getRating() < 1
                        || review.getRating() > 5
        ) {
            throw badRequest(
                    "Rating must be between 1 and 5."
            );
        }

        if (
                review.getReviewerDisplayPreference()
                        == null
        ) {
            throw badRequest(
                    "Reviewer display preference is required."
            );
        }

        if (
                review.getReviewerDisplayPreference()
                        == CustomerReviewDisplayPreference.CUSTOM

                        && normalizeOptional(
                        review.getReviewerDisplayName()
                ) == null
        ) {
            throw badRequest(
                    "A custom reviewer display name is required when CUSTOM is selected."
            );
        }

        String reviewText =
                normalizeRequired(
                        review.getReviewText(),
                        "Review text"
                );

        if (
                reviewText.length()
                        > 10000
        ) {
            throw badRequest(
                    "Review text must not exceed 10,000 characters."
            );
        }
    }

    // =================================================================
    // DISPLAY NAME RESOLUTION
    // =================================================================

    private String resolveDisplayNameFromCustomer(
            CustomerReview requestedReview,
            Customer customer
    ) {
        CustomerReviewDisplayPreference preference =
                requestedReview
                        .getReviewerDisplayPreference();

        if (
                preference
                        == CustomerReviewDisplayPreference.ANONYMOUS
        ) {
            return null;
        }

        if (
                preference
                        == CustomerReviewDisplayPreference.CUSTOM
        ) {
            return normalizeRequired(
                    requestedReview
                            .getReviewerDisplayName(),
                    "Custom reviewer display name"
            );
        }

        return normalizeRequired(
                customer.getDisplayName(),
                "Customer display name"
        );
    }

    private String resolveUpdatedDisplayName(
            CustomerReview requestedUpdate,
            CustomerReview existingReview
    ) {
        CustomerReviewDisplayPreference preference =
                requestedUpdate
                        .getReviewerDisplayPreference();

        if (
                preference
                        == CustomerReviewDisplayPreference.ANONYMOUS
        ) {
            return null;
        }

        if (
                preference
                        == CustomerReviewDisplayPreference.CUSTOM
        ) {
            return normalizeRequired(
                    requestedUpdate
                            .getReviewerDisplayName(),
                    "Custom reviewer display name"
            );
        }

        BookingRequest bookingRequest =
                existingReview
                        .getBookingRequest();

        if (bookingRequest == null) {
            throw conflict(
                    "Customer review is not associated with a completed booking."
            );
        }

        Customer customer =
                getRequiredBookingCustomer(
                        bookingRequest
                );

        return normalizeRequired(
                customer.getDisplayName(),
                "Customer display name"
        );
    }

    // =================================================================
    // SPAM SCORE VALIDATION
    // =================================================================

    private void validateSpamScore(
            BigDecimal spamScore
    ) {
        if (spamScore == null) {
            return;
        }

        if (
                spamScore.compareTo(
                        BigDecimal.ZERO
                ) < 0

                        || spamScore.compareTo(
                        BigDecimal.valueOf(
                                100
                        )
                ) > 0
        ) {
            throw badRequest(
                    "Spam score must be between 0 and 100."
            );
        }
    }

    // =================================================================
    // SAVE
    // =================================================================

    private CustomerReview saveReview(
            CustomerReview review,
            String conflictMessage
    ) {
        try {
            return customerReviewRepository
                    .saveAndFlush(
                            review
                    );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    conflictMessage,
                    exception
            );
        }
    }

    // =================================================================
    // ADMINISTRATOR
    // =================================================================

    private AdminUser getRequiredAdministrator(
            UUID administratorId
    ) {
        requireIdentifier(
                administratorId,
                "Administrator ID"
        );

        return adminUserRepository
                .findById(
                        administratorId
                )
                .orElseThrow(
                        () -> notFound(
                                "Administrator account was not found."
                        )
                );
    }

    // =================================================================
    // SLUG
    // =================================================================

    private String normalizeSlug(
            String value
    ) {
        String normalized =
                normalizeRequired(
                        value,
                        "Service slug"
                )
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^a-z0-9]+",
                                "-"
                        )
                        .replaceAll(
                                "^-+|-+$",
                                ""
                        );

        if (
                normalized.isBlank()
        ) {
            throw badRequest(
                    "Service slug is required."
            );
        }

        return normalized;
    }

    // =================================================================
    // STRING HELPERS
    // =================================================================

    private String truncate(
            String value,
            int maximumLength
    ) {
        String normalized =
                normalizeOptional(
                        value
                );

        if (
                normalized == null
                        || normalized.length()
                        <= maximumLength
        ) {
            return normalized;
        }

        return normalized.substring(
                0,
                maximumLength
        );
    }

    private String normalizeRequired(
            String value,
            String fieldName
    ) {
        String normalized =
                normalizeOptional(
                        value
                );

        if (normalized == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }

        return normalized;
    }

    private String normalizeOptional(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    // =================================================================
    // GENERAL VALIDATION
    // =================================================================

    private void requireIdentifier(
            UUID identifier,
            String fieldName
    ) {
        if (identifier == null) {
            throw badRequest(
                    fieldName + " is required."
            );
        }
    }

    private void requirePageable(
            Pageable pageable
    ) {
        if (pageable == null) {
            throw badRequest(
                    "Pagination information is required."
            );
        }
    }

    // =================================================================
    // HTTP EXCEPTIONS
    // =================================================================

    private ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException notFound(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                message
        );
    }

    private ResponseStatusException conflict(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}