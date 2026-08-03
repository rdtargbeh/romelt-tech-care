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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.CustomerReviewInvitationCreateRequest;
import romelt_techcare.backend.dto.CustomerReviewInvitationCreatedResponse;
import romelt_techcare.backend.dto.CustomerReviewInvitationResponse;
import romelt_techcare.backend.entity.CustomerReviewInvitation;
import romelt_techcare.backend.enums.CustomerReviewInvitationStatus;
import romelt_techcare.backend.mapper.CustomerReviewInvitationMapper;
import romelt_techcare.backend.service.CustomerReviewInvitationService;

import java.util.UUID;

/**
 * Protected administrator endpoints for review invitations.
 */
@RestController
@RequestMapping("/api/v1/admin/customer-review-invitations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
public class AdminCustomerReviewInvitationController {

    private final CustomerReviewInvitationService
            invitationService;

    private final CustomerReviewInvitationMapper
            invitationMapper;

    @PostMapping
    public ResponseEntity<
            ApiResponse<CustomerReviewInvitationCreatedResponse>
            > createInvitation(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            CustomerReviewInvitationCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        CustomerReviewInvitationCreatedResponse response =
                invitationService.createInvitation(
                        request.bookingRequestId(),
                        request.expiresAt(),
                        requireAdministratorId(principal)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Customer review invitation created successfully.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<CustomerReviewInvitationResponse>>
            > searchInvitations(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            CustomerReviewInvitationStatus status,

            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<CustomerReviewInvitationResponse> response =
                invitationService
                        .searchInvitations(
                                keyword,
                                status,
                                pageable
                        )
                        .map(invitationMapper::toResponse);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Customer review invitations retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/{reviewInvitationId}")
    public ResponseEntity<
            ApiResponse<CustomerReviewInvitationResponse>
            > getInvitation(
            @PathVariable
            UUID reviewInvitationId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Customer review invitation retrieved successfully.",
                invitationService.getInvitation(
                        reviewInvitationId
                ),
                httpRequest
        );
    }

    @GetMapping("/booking/{bookingRequestId}")
    public ResponseEntity<
            ApiResponse<Page<CustomerReviewInvitationResponse>>
            > getBookingInvitations(
            @PathVariable
            UUID bookingRequestId,

            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<CustomerReviewInvitationResponse> response =
                invitationService
                        .getBookingInvitations(
                                bookingRequestId,
                                pageable
                        )
                        .map(invitationMapper::toResponse);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Booking review invitations retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @PatchMapping("/{reviewInvitationId}/sent")
    public ResponseEntity<
            ApiResponse<CustomerReviewInvitationResponse>
            > markInvitationSent(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID reviewInvitationId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Customer review invitation marked as sent.",
                invitationService.markInvitationSent(
                        reviewInvitationId,
                        requireAdministratorId(principal)
                ),
                httpRequest
        );
    }

    @PatchMapping("/{reviewInvitationId}/revoke")
    public ResponseEntity<
            ApiResponse<CustomerReviewInvitationResponse>
            > revokeInvitation(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID reviewInvitationId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Customer review invitation revoked successfully.",
                invitationService.revokeInvitation(
                        reviewInvitationId,
                        requireAdministratorId(principal)
                ),
                httpRequest
        );
    }

    @PostMapping("/expire")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Integer>>
    expireInvitations(
            HttpServletRequest httpRequest
    ) {
        int expiredCount =
                invitationService.expireInvitations();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Expired review invitations processed successfully.",
                        expiredCount,
                        httpRequest.getRequestURI()
                )
        );
    }

    private ResponseEntity<
            ApiResponse<CustomerReviewInvitationResponse>
            > successfulResponse(
            String message,
            CustomerReviewInvitation invitation,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        invitationMapper.toResponse(invitation),
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