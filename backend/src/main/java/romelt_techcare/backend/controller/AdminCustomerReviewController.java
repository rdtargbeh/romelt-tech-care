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
 * Exposes administrator endpoints for review creation, moderation,
 * publication, hiding, archival, and response management.
 *
 * Base endpoint:
 * /api/v1/admin/customer-reviews
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/customer-reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
public class AdminCustomerReviewController {

    private final CustomerReviewService customerReviewService;

    private final CustomerReviewMapper customerReviewMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    createReview(
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
                                request.contactInquiryId(),
                                request.serviceId(),
                                request.customerPhotoMediaId(),
                                requireAdministratorId(principal)
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

    @PutMapping("/{customerReviewId}")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    updateReview(
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
                customerReviewService.updateReview(
                        customerReviewId,
                        customerReviewMapper
                                .toUpdateEntity(request),
                        request.serviceId(),
                        request.customerPhotoMediaId(),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Customer review updated successfully.",
                review,
                httpRequest
        );
    }

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

    @GetMapping("/{customerReviewId}")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    getReview(
            @PathVariable
            UUID customerReviewId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Customer review retrieved successfully.",
                customerReviewService
                        .getReview(customerReviewId),
                httpRequest
        );
    }

    @PatchMapping("/{customerReviewId}/approve")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    approveReview(
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
                customerReviewService.approveReview(
                        customerReviewId,
                        request == null
                                ? null
                                : request.moderationNotes(),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Customer review approved successfully.",
                review,
                httpRequest
        );
    }

    @PatchMapping("/{customerReviewId}/reject")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    rejectReview(
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
                customerReviewService.rejectReview(
                        customerReviewId,
                        request.rejectionReason(),
                        request.moderationNotes(),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Customer review rejected successfully.",
                review,
                httpRequest
        );
    }

    @PatchMapping("/{customerReviewId}/spam")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    markReviewAsSpam(
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
                customerReviewService.markReviewAsSpam(
                        customerReviewId,
                        request.spamScore(),
                        request.moderationNotes(),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Customer review marked as spam.",
                review,
                httpRequest
        );
    }

    @PatchMapping("/{customerReviewId}/publish")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    publishReview(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            @RequestBody(required = false)
            CustomerReviewPublicationRequest request,

            HttpServletRequest httpRequest
    ) {
        CustomerReview review =
                customerReviewService.publishReview(
                        customerReviewId,
                        request != null
                                && Boolean.TRUE.equals(
                                request.isFeatured()
                        ),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Customer review published successfully.",
                review,
                httpRequest
        );
    }

    @PatchMapping("/{customerReviewId}/unpublish")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    unpublishReview(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Customer review unpublished successfully.",
                customerReviewService.unpublishReview(
                        customerReviewId,
                        requireAdministratorId(principal)
                ),
                httpRequest
        );
    }

    @PatchMapping("/{customerReviewId}/featured")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    updateFeaturedStatus(
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
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Customer review featured status updated successfully.",
                review,
                httpRequest
        );
    }

    @PatchMapping("/{customerReviewId}/hide")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    hideReview(
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
                customerReviewService.hideReview(
                        customerReviewId,
                        request == null
                                ? null
                                : request.moderationNotes(),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Customer review hidden successfully.",
                review,
                httpRequest
        );
    }

    @PatchMapping("/{customerReviewId}/archive")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    archiveReview(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Customer review archived successfully.",
                customerReviewService.archiveReview(
                        customerReviewId,
                        requireAdministratorId(principal)
                ),
                httpRequest
        );
    }

    @PutMapping("/{customerReviewId}/response")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    addAdminResponse(
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
                customerReviewService.addAdminResponse(
                        customerReviewId,
                        request.adminResponse(),
                        requireAdministratorId(principal)
                ),
                httpRequest
        );
    }

    @DeleteMapping("/{customerReviewId}/response")
    public ResponseEntity<ApiResponse<CustomerReviewResponse>>
    removeAdminResponse(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID customerReviewId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Administrator response removed successfully.",
                customerReviewService.removeAdminResponse(
                        customerReviewId,
                        requireAdministratorId(principal)
                ),
                httpRequest
        );
    }

    private ResponseEntity<ApiResponse<CustomerReviewResponse>>
    successfulResponse(
            String message,
            CustomerReview review,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        customerReviewMapper.toResponse(review),
                        request.getRequestURI()
                )
        );
    }

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