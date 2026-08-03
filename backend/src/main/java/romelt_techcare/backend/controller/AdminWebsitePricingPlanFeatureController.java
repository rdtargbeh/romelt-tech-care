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
import romelt_techcare.backend.dto.WebsitePricingPlanFeatureCreateRequest;
import romelt_techcare.backend.dto.WebsitePricingPlanFeatureOrderRequest;
import romelt_techcare.backend.dto.WebsitePricingPlanFeatureResponse;
import romelt_techcare.backend.dto.WebsitePricingPlanFeatureStatusRequest;
import romelt_techcare.backend.dto.WebsitePricingPlanFeatureUpdateRequest;
import romelt_techcare.backend.entity.WebsitePricingPlanFeature;
import romelt_techcare.backend.mapper.WebsitePricingPlanFeatureMapper;
import romelt_techcare.backend.service.WebsitePricingPlanFeatureService;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN PRICING PLAN FEATURE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing features
 * belonging to draft website pricing-plan versions.
 *
 * Responsibilities:
 * - Creates pricing-plan features.
 * - Updates feature text, icon, order, and active state.
 * - Retrieves and searches pricing-plan features.
 * - Activates and deactivates features.
 * - Updates feature display order.
 * - Permanently deletes draft-owned features.
 *
 * Immutability:
 * Features attached to published or archived pricing-plan versions
 * cannot be modified.
 *
 * Base endpoint:
 * /api/v1/admin/website-pricing-plan-features
 * ================================================================
 */
@RestController
@RequestMapping(
        "/api/v1/admin/website-pricing-plan-features"
)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsitePricingPlanFeatureController {

    private final WebsitePricingPlanFeatureService
            websitePricingPlanFeatureService;

    private final WebsitePricingPlanFeatureMapper
            websitePricingPlanFeatureMapper;

    @PostMapping("/version/{pricingPlanVersionId}")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanFeatureResponse>
            > createPricingPlanFeature(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanVersionId,

            @Valid
            @RequestBody
            WebsitePricingPlanFeatureCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlanFeature feature =
                websitePricingPlanFeatureService
                        .createPricingPlanFeature(
                                pricingPlanVersionId,
                                websitePricingPlanFeatureMapper
                                        .toEntity(request),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website pricing-plan feature created successfully.",
                                websitePricingPlanFeatureMapper
                                        .toResponse(feature),
                                httpRequest.getRequestURI()
                        )
                );
    }

    @PutMapping("/{pricingPlanFeatureId}")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanFeatureResponse>
            > updatePricingPlanFeature(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanFeatureId,

            @Valid
            @RequestBody
            WebsitePricingPlanFeatureUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlanFeature feature =
                websitePricingPlanFeatureService
                        .updatePricingPlanFeature(
                                pricingPlanFeatureId,
                                websitePricingPlanFeatureMapper
                                        .toUpdateEntity(request),
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website pricing-plan feature updated successfully.",
                feature,
                httpRequest
        );
    }

    @GetMapping("/{pricingPlanFeatureId}")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanFeatureResponse>
            > getPricingPlanFeature(
            @PathVariable
            UUID pricingPlanFeatureId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website pricing-plan feature retrieved successfully.",
                websitePricingPlanFeatureService
                        .getPricingPlanFeature(
                                pricingPlanFeatureId
                        ),
                httpRequest
        );
    }

    @GetMapping("/version/{pricingPlanVersionId}")
    public ResponseEntity<
            ApiResponse<List<WebsitePricingPlanFeatureResponse>>
            > getPricingPlanFeatures(
            @PathVariable
            UUID pricingPlanVersionId,

            HttpServletRequest httpRequest
    ) {
        List<WebsitePricingPlanFeatureResponse> response =
                websitePricingPlanFeatureService
                        .getPricingPlanFeatures(
                                pricingPlanVersionId
                        )
                        .stream()
                        .map(
                                websitePricingPlanFeatureMapper
                                        ::toResponse
                        )
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website pricing-plan features retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping(
            "/version/{pricingPlanVersionId}/search"
    )
    public ResponseEntity<
            ApiResponse<Page<WebsitePricingPlanFeatureResponse>>
            > searchPricingPlanFeatures(
            @PathVariable
            UUID pricingPlanVersionId,

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            Boolean isActive,

            @PageableDefault(
                    size = 10,
                    sort = {
                            "displayOrder",
                            "createdAt"
                    },
                    direction = Sort.Direction.ASC
            )
            Pageable pageable,

            HttpServletRequest httpRequest
    ) {
        Page<WebsitePricingPlanFeatureResponse> response =
                websitePricingPlanFeatureService
                        .searchPricingPlanFeatures(
                                pricingPlanVersionId,
                                keyword,
                                isActive,
                                pageable
                        )
                        .map(
                                websitePricingPlanFeatureMapper
                                        ::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website pricing-plan features retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @PatchMapping("/{pricingPlanFeatureId}/status")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanFeatureResponse>
            > updatePricingPlanFeatureStatus(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanFeatureId,

            @Valid
            @RequestBody
            WebsitePricingPlanFeatureStatusRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlanFeature feature =
                websitePricingPlanFeatureService
                        .updatePricingPlanFeatureStatus(
                                pricingPlanFeatureId,
                                request.isActive(),
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website pricing-plan feature status updated successfully.",
                feature,
                httpRequest
        );
    }

    @PatchMapping("/{pricingPlanFeatureId}/order")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanFeatureResponse>
            > updatePricingPlanFeatureOrder(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanFeatureId,

            @Valid
            @RequestBody
            WebsitePricingPlanFeatureOrderRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlanFeature feature =
                websitePricingPlanFeatureService
                        .updatePricingPlanFeatureOrder(
                                pricingPlanFeatureId,
                                request.displayOrder(),
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website pricing-plan feature order updated successfully.",
                feature,
                httpRequest
        );
    }

    @DeleteMapping("/{pricingPlanFeatureId}")
    public ResponseEntity<Void> deletePricingPlanFeature(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanFeatureId
    ) {
        websitePricingPlanFeatureService
                .deletePricingPlanFeature(
                        pricingPlanFeatureId,
                        requireAdministratorId(principal)
                );

        return ResponseEntity.noContent().build();
    }

    @GetMapping(
            "/version/{pricingPlanVersionId}/counts/all"
    )
    public ResponseEntity<ApiResponse<Long>>
    countPricingPlanFeatures(
            @PathVariable
            UUID pricingPlanVersionId,

            HttpServletRequest httpRequest
    ) {
        long count =
                websitePricingPlanFeatureService
                        .countPricingPlanFeatures(
                                pricingPlanVersionId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website pricing-plan feature count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping(
            "/version/{pricingPlanVersionId}/counts/active"
    )
    public ResponseEntity<ApiResponse<Long>>
    countActivePricingPlanFeatures(
            @PathVariable
            UUID pricingPlanVersionId,

            HttpServletRequest httpRequest
    ) {
        long count =
                websitePricingPlanFeatureService
                        .countActivePricingPlanFeatures(
                                pricingPlanVersionId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Active website pricing-plan feature count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    private ResponseEntity<
            ApiResponse<WebsitePricingPlanFeatureResponse>
            > successfulResponse(
            String message,
            WebsitePricingPlanFeature feature,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        websitePricingPlanFeatureMapper
                                .toResponse(feature),
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