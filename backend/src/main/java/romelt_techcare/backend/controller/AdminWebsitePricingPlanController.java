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
import romelt_techcare.backend.dto.WebsitePricingPlanCreateRequest;
import romelt_techcare.backend.dto.WebsitePricingPlanResponse;
import romelt_techcare.backend.dto.WebsitePricingPlanStatusRequest;
import romelt_techcare.backend.dto.WebsitePricingPlanUpdateRequest;
import romelt_techcare.backend.entity.WebsitePricingPlan;
import romelt_techcare.backend.enums.WebsitePricingPlanStatus;
import romelt_techcare.backend.mapper.WebsitePricingPlanMapper;
import romelt_techcare.backend.service.WebsitePricingPlanService;

import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN PRICING PLAN CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing stable
 * website pricing-plan identities.
 *
 * Responsibilities:
 * - Creates pricing plans.
 * - Updates plan codes, slugs, and lifecycle status.
 * - Retrieves and searches pricing-plan identities.
 * - Activates, deactivates, and archives plans.
 * - Soft-deletes and restores plans.
 * - Returns status and deletion counts.
 *
 * Version lifecycle:
 * Draft creation, content editing, publication, version history, and
 * version archival will be handled by
 * AdminWebsitePricingPlanVersionController.
 *
 * This controller does not expose arbitrary draft or published
 * version-pointer assignment.
 *
 * Base endpoint:
 * /api/v1/admin/website-pricing-plans
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-pricing-plans")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsitePricingPlanController {

    private final WebsitePricingPlanService
            websitePricingPlanService;

    private final WebsitePricingPlanMapper
            websitePricingPlanMapper;

    @PostMapping
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanResponse>
            > createPricingPlan(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @Valid
            @RequestBody
            WebsitePricingPlanCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlan pricingPlan =
                websitePricingPlanService.createPricingPlan(
                        websitePricingPlanMapper.toEntity(request),
                        requireAdministratorId(principal)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website pricing plan created successfully.",
                                websitePricingPlanMapper.toResponse(
                                        pricingPlan
                                ),
                                httpRequest.getRequestURI()
                        )
                );
    }

    @PutMapping("/{pricingPlanId}")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanResponse>
            > updatePricingPlan(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanId,

            @Valid
            @RequestBody
            WebsitePricingPlanUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlan pricingPlan =
                websitePricingPlanService.updatePricingPlan(
                        pricingPlanId,
                        websitePricingPlanMapper.toUpdateEntity(
                                request
                        ),
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website pricing plan updated successfully.",
                pricingPlan,
                httpRequest
        );
    }

    @GetMapping
    public ResponseEntity<
            ApiResponse<Page<WebsitePricingPlanResponse>>
            > searchPricingPlans(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            WebsitePricingPlanStatus planStatus,

            @PageableDefault(
                    size = 10,
                    sort = "planCode",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsitePricingPlanResponse> response =
                websitePricingPlanService
                        .searchPricingPlans(
                                keyword,
                                planStatus,
                                pageable
                        )
                        .map(
                                websitePricingPlanMapper::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website pricing plans retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/deleted")
    public ResponseEntity<
            ApiResponse<Page<WebsitePricingPlanResponse>>
            > getDeletedPricingPlans(
            @PageableDefault(
                    size = 10,
                    sort = "deletedAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsitePricingPlanResponse> response =
                websitePricingPlanService
                        .getDeletedPricingPlans(pageable)
                        .map(
                                websitePricingPlanMapper::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Deleted website pricing plans retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/{pricingPlanId}")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanResponse>
            > getPricingPlan(
            @PathVariable
            UUID pricingPlanId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website pricing plan retrieved successfully.",
                websitePricingPlanService.getPricingPlan(
                        pricingPlanId
                ),
                httpRequest
        );
    }

    @GetMapping("/by-code/{planCode}")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanResponse>
            > getPricingPlanByCode(
            @PathVariable
            String planCode,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website pricing plan retrieved successfully.",
                websitePricingPlanService.getPricingPlanByCode(
                        planCode
                ),
                httpRequest
        );
    }

    @GetMapping("/by-slug/{planSlug}")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanResponse>
            > getPricingPlanBySlug(
            @PathVariable
            String planSlug,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website pricing plan retrieved successfully.",
                websitePricingPlanService.getPricingPlanBySlug(
                        planSlug
                ),
                httpRequest
        );
    }

    @PatchMapping("/{pricingPlanId}/status")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanResponse>
            > updatePricingPlanStatus(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanId,

            @Valid
            @RequestBody
            WebsitePricingPlanStatusRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlan pricingPlan =
                websitePricingPlanService
                        .updatePricingPlanStatus(
                                pricingPlanId,
                                request.planStatus(),
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website pricing-plan status updated successfully.",
                pricingPlan,
                httpRequest
        );
    }

    @PatchMapping("/{pricingPlanId}/activate")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanResponse>
            > activatePricingPlan(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanId,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlan pricingPlan =
                websitePricingPlanService.activatePricingPlan(
                        pricingPlanId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website pricing plan activated successfully.",
                pricingPlan,
                httpRequest
        );
    }

    @PatchMapping("/{pricingPlanId}/deactivate")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanResponse>
            > deactivatePricingPlan(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanId,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlan pricingPlan =
                websitePricingPlanService.deactivatePricingPlan(
                        pricingPlanId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website pricing plan deactivated successfully.",
                pricingPlan,
                httpRequest
        );
    }

    @PatchMapping("/{pricingPlanId}/archive")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanResponse>
            > archivePricingPlan(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanId,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlan pricingPlan =
                websitePricingPlanService.archivePricingPlan(
                        pricingPlanId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website pricing plan archived successfully.",
                pricingPlan,
                httpRequest
        );
    }

    @DeleteMapping("/{pricingPlanId}")
    public ResponseEntity<Void> deletePricingPlan(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanId
    ) {
        websitePricingPlanService.deletePricingPlan(
                pricingPlanId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{pricingPlanId}/restore")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanResponse>
            > restorePricingPlan(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanId,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlan pricingPlan =
                websitePricingPlanService.restorePricingPlan(
                        pricingPlanId,
                        requireAdministratorId(principal)
                );

        return successfulResponse(
                "Website pricing plan restored successfully.",
                pricingPlan,
                httpRequest
        );
    }

    @GetMapping("/counts/status/{planStatus}")
    public ResponseEntity<ApiResponse<Long>>
    countPricingPlansByStatus(
            @PathVariable
            WebsitePricingPlanStatus planStatus,

            HttpServletRequest httpRequest
    ) {
        long count =
                websitePricingPlanService
                        .countPricingPlansByStatus(planStatus);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website pricing-plan count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/counts/deleted")
    public ResponseEntity<ApiResponse<Long>>
    countDeletedPricingPlans(
            HttpServletRequest httpRequest
    ) {
        long count =
                websitePricingPlanService
                        .countDeletedPricingPlans();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Deleted website pricing-plan count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    private ResponseEntity<
            ApiResponse<WebsitePricingPlanResponse>
            > successfulResponse(
            String message,
            WebsitePricingPlan pricingPlan,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        websitePricingPlanMapper.toResponse(
                                pricingPlan
                        ),
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