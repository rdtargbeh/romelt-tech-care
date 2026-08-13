package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.CustomerReviewAdminCreateRequest;
import romelt_techcare.backend.dto.CustomerReviewAdminResponseRequest;
import romelt_techcare.backend.dto.CustomerReviewApprovalRequest;
import romelt_techcare.backend.dto.CustomerReviewEligibleBookingResponse;
import romelt_techcare.backend.dto.CustomerReviewHideRequest;
import romelt_techcare.backend.dto.CustomerReviewPublicationRequest;
import romelt_techcare.backend.dto.CustomerReviewRejectionRequest;
import romelt_techcare.backend.dto.CustomerReviewResponse;
import romelt_techcare.backend.dto.CustomerReviewSpamRequest;
import romelt_techcare.backend.dto.CustomerReviewUpdateRequest;
import romelt_techcare.backend.entity.CustomerReview;
import romelt_techcare.backend.enums.CustomerReviewModerationStatus;
import romelt_techcare.backend.enums.CustomerReviewSource;
import romelt_techcare.backend.mapper.CustomerReviewMapper;
import romelt_techcare.backend.service.CustomerReviewService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN CUSTOMER REVIEW CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes authenticated administrator endpoints for customer-review
 * creation, editing, moderation, publication, hiding, archival,
 * administrator responses, and eligible completed-booking selection.
 *
 * Base endpoint:
 * /api/v1/admin/customer-reviews
 *
 * Core review rule:
 * A Romelt TechCare customer review represents feedback about an
 * actual service received through a completed, review-eligible
 * BookingRequest.
 *
 * Review creation:
 * - bookingRequestId is supplied by the administrator request.
 * - Customer identity is NOT supplied by the browser.
 * - Customer email and phone are NOT supplied by the browser.
 * - serviceId is NOT supplied by the browser.
 * - verified-customer status is NOT supplied by the browser.
 *
 * CustomerReviewService resolves those authoritative values from:
 *
 * BookingRequest
 *      -> customerId
 *      -> Customer
 *
 * BookingRequest
 *      -> serviceId
 *      -> WebsiteService
 *
 * The administrator supplies only review-specific information such
 * as rating, review text, source, display preference, external source
 * URL, consent information, and optional customer photo.
 *
 * Review source:
 * CustomerReviewSource describes where feedback was communicated,
 * such as BOOKING_FOLLOW_UP, PHONE, EMAIL, GOOGLE, FACEBOOK, or OTHER.
 *
 * Review source does not prove that a service was received. The
 * completed booking establishes the service relationship.
 *
 * Eligible-booking selector:
 * GET /api/v1/admin/customer-reviews/eligible-bookings
 *
 * Returns completed bookings that:
 * - have status COMPLETED;
 * - have completedAt populated;
 * - are reviewEligible;
 * - are linked to a Customer;
 * - are linked to a WebsiteService;
 * - do not already have a CustomerReview.
 *
 * Update protection:
 * Existing review updates cannot replace the review's booking,
 * customer identity, customer email, telephone number, service, or
 * verified-customer status.
 *
 * Authorization:
 * SUPER_ADMIN, ADMIN, and STAFF may access these endpoints.
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/customer-reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
public class AdminCustomerReviewController {

    private final CustomerReviewService
            customerReviewService;

    private final CustomerReviewMapper
            customerReviewMapper;

    // =================================================================
    // CREATE REVIEW
    // =================================================================

    /**
     * Records customer feedback for one completed, review-eligible
     * Romelt TechCare booking.
     *
     * Customer/service identity is resolved by the service layer from
     * request.bookingRequestId().
     */
    @PostMapping
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > createReview(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            CustomerReviewAdminCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        CustomerReview review =
                customerReviewService
                        .createReviewByAdministrator(
                                customerReviewMapper
                                        .toEntity(request),

                                request.bookingRequestId(),

                                request.customerPhotoMediaId(),

                                requireAdministratorId(
                                        principal
                                )
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Customer review created successfully.",
                                customerReviewMapper
                                        .toResponse(review),
                                httpRequest.getRequestURI()
                        )
                );
    }

    // =================================================================
    // UPDATE REVIEW
    // =================================================================

    /**
     * Updates editable review content.
     *
     * Booking, customer identity, customer contact information,
     * WebsiteService, and verified-customer status remain protected
     * by the service layer and cannot be changed here.
     */
    @PutMapping("/{customerReviewId}")
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > updateReview(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            @Valid
            @RequestBody
            CustomerReviewUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        CustomerReview review =
                customerReviewService
                        .updateReview(
                                customerReviewId,

                                customerReviewMapper
                                        .toUpdateEntity(request),

                                request.customerPhotoMediaId(),

                                requireAdministratorId(
                                        principal
                                )
                        );

        return successfulResponse(
                "Customer review updated successfully.",
                review,
                httpRequest
        );
    }

    // =================================================================
// ELIGIBLE COMPLETED BOOKINGS
// =================================================================

    /**
     * Returns completed bookings belonging to one selected customer
     * that may currently receive a customer review.
     *
     * Customer-first workflow:
     *
     * 1. Administrator searches for and selects a Customer.
     * 2. customerId is passed to this endpoint.
     * 3. Only that customer's eligible completed bookings are returned.
     * 4. Administrator selects the exact completed booking.
     *
     * Eligibility requires:
     * - booking.customerId matches the selected customerId;
     * - status = COMPLETED;
     * - completedAt is populated;
     * - reviewEligible = true;
     * - serviceId is populated;
     * - no CustomerReview already exists for the booking.
     *
     * keyword is optional and may further narrow the selected customer's
     * eligible bookings.
     */
    @GetMapping("/eligible-bookings")
    public ResponseEntity<
            ApiResponse<Page<CustomerReviewEligibleBookingResponse>>
            > getEligibleBookings(
            @RequestParam
            UUID customerId,

            @RequestParam(required = false)
            String keyword,

            @PageableDefault(
                    size = 10,
                    sort = "completedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<CustomerReviewEligibleBookingResponse> response =
                customerReviewService
                        .getEligibleBookings(
                                customerId,
                                keyword,
                                pageable
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Eligible completed bookings retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    // =================================================================
    // SEARCH REVIEWS
    // =================================================================

    /**
     * Returns administrator-visible customer reviews using optional
     * filters.
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<CustomerReviewResponse>>
            > searchReviews(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            CustomerReviewModerationStatus moderationStatus,

            @RequestParam(required = false)
            CustomerReviewSource reviewSource,

            @RequestParam(required = false)
            Short rating,

            @RequestParam(required = false)
            UUID serviceId,

            @RequestParam(required = false)
            Boolean isVerifiedCustomer,

            @RequestParam(required = false)
            Boolean isPublic,

            @RequestParam(required = false)
            Boolean isFeatured,

            @RequestParam(required = false)
            Boolean isSpam,

            @PageableDefault(
                    size = 10,
                    sort = "submittedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<CustomerReviewResponse> response =
                customerReviewService
                        .searchReviews(
                                keyword,
                                moderationStatus,
                                reviewSource,
                                rating,
                                serviceId,
                                isVerifiedCustomer,
                                isPublic,
                                isFeatured,
                                isSpam,
                                pageable
                        )
                        .map(
                                customerReviewMapper::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Customer reviews retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    // =================================================================
    // GET ONE
    // =================================================================

    @GetMapping("/{customerReviewId}")
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > getReview(
            @PathVariable
            UUID customerReviewId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Customer review retrieved successfully.",
                customerReviewService
                        .getReview(
                                customerReviewId
                        ),
                httpRequest
        );
    }

    // =================================================================
    // APPROVE
    // =================================================================

    @PatchMapping("/{customerReviewId}/approve")
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > approveReview(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            @Valid
            @RequestBody(required = false)
            CustomerReviewApprovalRequest request,

            HttpServletRequest httpRequest
    ) {
        CustomerReview review =
                customerReviewService
                        .approveReview(
                                customerReviewId,

                                request == null
                                        ? null
                                        : request.moderationNotes(),

                                requireAdministratorId(
                                        principal
                                )
                        );

        return successfulResponse(
                "Customer review approved successfully.",
                review,
                httpRequest
        );
    }

    // =================================================================
    // REJECT
    // =================================================================

    @PatchMapping("/{customerReviewId}/reject")
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > rejectReview(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            @Valid
            @RequestBody
            CustomerReviewRejectionRequest request,

            HttpServletRequest httpRequest
    ) {
        CustomerReview review =
                customerReviewService
                        .rejectReview(
                                customerReviewId,
                                request.rejectionReason(),
                                request.moderationNotes(),
                                requireAdministratorId(
                                        principal
                                )
                        );

        return successfulResponse(
                "Customer review rejected successfully.",
                review,
                httpRequest
        );
    }

    // =================================================================
    // SPAM
    // =================================================================

    @PatchMapping("/{customerReviewId}/spam")
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > markReviewAsSpam(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            @Valid
            @RequestBody
            CustomerReviewSpamRequest request,

            HttpServletRequest httpRequest
    ) {
        CustomerReview review =
                customerReviewService
                        .markReviewAsSpam(
                                customerReviewId,
                                request.spamScore(),
                                request.moderationNotes(),
                                requireAdministratorId(
                                        principal
                                )
                        );

        return successfulResponse(
                "Customer review marked as spam.",
                review,
                httpRequest
        );
    }

    // =================================================================
    // PUBLISH
    // =================================================================

    @PatchMapping("/{customerReviewId}/publish")
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > publishReview(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            @RequestBody(required = false)
            CustomerReviewPublicationRequest request,

            HttpServletRequest httpRequest
    ) {
        CustomerReview review =
                customerReviewService
                        .publishReview(
                                customerReviewId,

                                request != null
                                        && Boolean.TRUE.equals(
                                        request.isFeatured()
                                ),

                                requireAdministratorId(
                                        principal
                                )
                        );

        return successfulResponse(
                "Customer review published successfully.",
                review,
                httpRequest
        );
    }

    // =================================================================
    // UNPUBLISH
    // =================================================================

    @PatchMapping("/{customerReviewId}/unpublish")
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > unpublishReview(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Customer review unpublished successfully.",

                customerReviewService
                        .unpublishReview(
                                customerReviewId,
                                requireAdministratorId(
                                        principal
                                )
                        ),

                httpRequest
        );
    }

    // =================================================================
    // FEATURED STATUS
    // =================================================================

    @PatchMapping("/{customerReviewId}/featured")
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > updateFeaturedStatus(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            @RequestBody
            CustomerReviewPublicationRequest request,

            HttpServletRequest httpRequest
    ) {
        CustomerReview review =
                customerReviewService
                        .updateFeaturedStatus(
                                customerReviewId,

                                Boolean.TRUE.equals(
                                        request.isFeatured()
                                ),

                                requireAdministratorId(
                                        principal
                                )
                        );

        return successfulResponse(
                "Customer review featured status updated successfully.",
                review,
                httpRequest
        );
    }

    // =================================================================
    // HIDE
    // =================================================================

    @PatchMapping("/{customerReviewId}/hide")
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > hideReview(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            @Valid
            @RequestBody(required = false)
            CustomerReviewHideRequest request,

            HttpServletRequest httpRequest
    ) {
        CustomerReview review =
                customerReviewService
                        .hideReview(
                                customerReviewId,

                                request == null
                                        ? null
                                        : request.moderationNotes(),

                                requireAdministratorId(
                                        principal
                                )
                        );

        return successfulResponse(
                "Customer review hidden successfully.",
                review,
                httpRequest
        );
    }

    // =================================================================
    // ARCHIVE
    // =================================================================

    @PatchMapping("/{customerReviewId}/archive")
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > archiveReview(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Customer review archived successfully.",

                customerReviewService
                        .archiveReview(
                                customerReviewId,
                                requireAdministratorId(
                                        principal
                                )
                        ),

                httpRequest
        );
    }

    // =================================================================
    // ADMIN RESPONSE
    // =================================================================

    @PutMapping("/{customerReviewId}/response")
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > addAdminResponse(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            @Valid
            @RequestBody
            CustomerReviewAdminResponseRequest request,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Administrator response saved successfully.",

                customerReviewService
                        .addAdminResponse(
                                customerReviewId,
                                request.adminResponse(),
                                requireAdministratorId(
                                        principal
                                )
                        ),

                httpRequest
        );
    }

    @DeleteMapping("/{customerReviewId}/response")
    public ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > removeAdminResponse(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Administrator response removed successfully.",

                customerReviewService
                        .removeAdminResponse(
                                customerReviewId,
                                requireAdministratorId(
                                        principal
                                )
                        ),

                httpRequest
        );
    }

    // =================================================================
    // RESPONSE HELPER
    // =================================================================

    private ResponseEntity<
            ApiResponse<CustomerReviewResponse>
            > successfulResponse(
            String message,
            CustomerReview review,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        customerReviewMapper
                                .toResponse(review),
                        request.getRequestURI()
                )
        );
    }

    // =================================================================
    // AUTHENTICATION HELPER
    // =================================================================

    private UUID requireAdministratorId(
            AdminJwtPrincipal principal
    ) {
        if (
                principal == null
                        || principal.adminUserId() == null
        ) {
            throw new IllegalArgumentException(
                    "Authenticated administrator information is required."
            );
        }

        return principal.adminUserId();
    }
}