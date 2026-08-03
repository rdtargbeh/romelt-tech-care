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
import romelt_techcare.backend.dto.WebsitePricingPlanVersionCloneRequest;
import romelt_techcare.backend.dto.WebsitePricingPlanVersionCreateRequest;
import romelt_techcare.backend.dto.WebsitePricingPlanVersionResponse;
import romelt_techcare.backend.dto.WebsitePricingPlanVersionUpdateRequest;
import romelt_techcare.backend.entity.WebsitePricingPlanVersion;
import romelt_techcare.backend.enums.WebsitePricingPlanVersionStatus;
import romelt_techcare.backend.mapper.WebsitePricingPlanVersionMapper;
import romelt_techcare.backend.service.WebsitePricingPlanVersionService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN PRICING PLAN VERSION CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for pricing-plan draft
 * creation, editing, publishing, archival, deletion, and history.
 *
 * Base endpoint:
 * /api/v1/admin/website-pricing-plan-versions
 * ================================================================
 */
@RestController
@RequestMapping(
        "/api/v1/admin/website-pricing-plan-versions"
)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsitePricingPlanVersionController {

    private final WebsitePricingPlanVersionService
            websitePricingPlanVersionService;

    private final WebsitePricingPlanVersionMapper
            websitePricingPlanVersionMapper;

    @PostMapping("/plan/{pricingPlanId}/draft")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanVersionResponse>
            > createDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanId,

            @Valid
            @RequestBody
            WebsitePricingPlanVersionCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlanVersion version =
                websitePricingPlanVersionService
                        .createDraft(
                                pricingPlanId,
                                websitePricingPlanVersionMapper
                                        .toEntity(request),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website pricing-plan draft created successfully.",
                                websitePricingPlanVersionMapper
                                        .toResponse(version),
                                httpRequest.getRequestURI()
                        )
                );
    }

    @PostMapping(
            "/plan/{pricingPlanId}/draft-from-published"
    )
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanVersionResponse>
            > createDraftFromPublishedVersion(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanId,

            @Valid
            @RequestBody(required = false)
            WebsitePricingPlanVersionCloneRequest request,

            HttpServletRequest httpRequest
    ) {
        String changeSummary =
                request == null
                        ? null
                        : request.changeSummary();

        WebsitePricingPlanVersion version =
                websitePricingPlanVersionService
                        .createDraftFromPublishedVersion(
                                pricingPlanId,
                                changeSummary,
                                requireAdministratorId(principal)
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Draft created from the published pricing-plan version.",
                                websitePricingPlanVersionMapper
                                        .toResponse(version),
                                httpRequest.getRequestURI()
                        )
                );
    }

    @PutMapping("/{pricingPlanVersionId}")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanVersionResponse>
            > updateDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanVersionId,

            @Valid
            @RequestBody
            WebsitePricingPlanVersionUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlanVersion version =
                websitePricingPlanVersionService
                        .updateDraft(
                                pricingPlanVersionId,
                                websitePricingPlanVersionMapper
                                        .toUpdateEntity(request),
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website pricing-plan draft updated successfully.",
                version,
                httpRequest
        );
    }

    @GetMapping("/{pricingPlanVersionId}")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanVersionResponse>
            > getPricingPlanVersion(
            @PathVariable
            UUID pricingPlanVersionId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website pricing-plan version retrieved successfully.",
                websitePricingPlanVersionService
                        .getPricingPlanVersion(
                                pricingPlanVersionId
                        ),
                httpRequest
        );
    }

    @GetMapping("/plan/{pricingPlanId}/draft")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanVersionResponse>
            > getCurrentDraft(
            @PathVariable
            UUID pricingPlanId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Current website pricing-plan draft retrieved successfully.",
                websitePricingPlanVersionService
                        .getCurrentDraft(pricingPlanId),
                httpRequest
        );
    }

    @GetMapping("/plan/{pricingPlanId}/published")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanVersionResponse>
            > getCurrentPublishedVersion(
            @PathVariable
            UUID pricingPlanId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Current published pricing-plan version retrieved successfully.",
                websitePricingPlanVersionService
                        .getCurrentPublishedVersion(
                                pricingPlanId
                        ),
                httpRequest
        );
    }

    @GetMapping("/plan/{pricingPlanId}/history")
    public ResponseEntity<
            ApiResponse<
                    Page<WebsitePricingPlanVersionResponse>
                    >
            > getVersionHistory(
            @PathVariable
            UUID pricingPlanId,

            @RequestParam(required = false)
            WebsitePricingPlanVersionStatus versionStatus,

            @PageableDefault(
                    size = 10,
                    sort = "versionNumber",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsitePricingPlanVersionResponse> response =
                websitePricingPlanVersionService
                        .getVersionHistory(
                                pricingPlanId,
                                versionStatus,
                                pageable
                        )
                        .map(
                                websitePricingPlanVersionMapper
                                        ::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website pricing-plan version history retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @PatchMapping("/{pricingPlanVersionId}/publish")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanVersionResponse>
            > publishDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanVersionId,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlanVersion version =
                websitePricingPlanVersionService
                        .publishDraft(
                                pricingPlanVersionId,
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website pricing plan published successfully.",
                version,
                httpRequest
        );
    }

    @PatchMapping("/{pricingPlanVersionId}/archive")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanVersionResponse>
            > archiveVersion(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanVersionId,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlanVersion version =
                websitePricingPlanVersionService
                        .archiveVersion(
                                pricingPlanVersionId,
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website pricing-plan version archived successfully.",
                version,
                httpRequest
        );
    }

    @DeleteMapping("/{pricingPlanVersionId}/draft")
    public ResponseEntity<Void> deleteDraft(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanVersionId
    ) {
        websitePricingPlanVersionService.deleteDraft(
                pricingPlanVersionId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<
            ApiResponse<WebsitePricingPlanVersionResponse>
            > successfulResponse(
            String message,
            WebsitePricingPlanVersion version,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        websitePricingPlanVersionMapper
                                .toResponse(version),
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