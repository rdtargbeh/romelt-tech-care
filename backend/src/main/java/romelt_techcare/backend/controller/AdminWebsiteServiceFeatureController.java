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
import romelt_techcare.backend.dto.WebsiteServiceFeatureCreateRequest;
import romelt_techcare.backend.dto.WebsiteServiceFeatureOrderRequest;
import romelt_techcare.backend.dto.WebsiteServiceFeatureResponse;
import romelt_techcare.backend.dto.WebsiteServiceFeatureStatusRequest;
import romelt_techcare.backend.dto.WebsiteServiceFeatureUpdateRequest;
import romelt_techcare.backend.entity.WebsiteServiceFeature;
import romelt_techcare.backend.mapper.WebsiteServiceFeatureMapper;
import romelt_techcare.backend.service.WebsiteServiceFeatureService;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN SERVICE FEATURE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes protected administrator endpoints for managing features
 * belonging to draft website-service versions.
 *
 * Responsibilities:
 * - Creates service-version features.
 * - Updates feature text, icon, order, and active state.
 * - Retrieves and searches service features.
 * - Activates and deactivates features.
 * - Updates display order.
 * - Deletes draft-owned features.
 *
 * Immutability:
 * Features attached to published or archived service versions cannot
 * be modified through these endpoints.
 *
 * Base endpoint:
 * /api/v1/admin/website-service-features
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/admin/website-service-features")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsiteServiceFeatureController {

    private final WebsiteServiceFeatureService
            websiteServiceFeatureService;

    private final WebsiteServiceFeatureMapper
            websiteServiceFeatureMapper;

    /**
     * Creates a feature for one draft service version.
     *
     * POST /api/v1/admin/website-service-features/version/{serviceVersionId}
     */
    @PostMapping("/version/{serviceVersionId}")
    public ResponseEntity<
            ApiResponse<WebsiteServiceFeatureResponse>
            > createServiceFeature(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceVersionId,

            @Valid
            @RequestBody
            WebsiteServiceFeatureCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteServiceFeature feature =
                websiteServiceFeatureService
                        .createServiceFeature(
                                serviceVersionId,
                                websiteServiceFeatureMapper
                                        .toEntity(request),
                                requireAdministratorId(principal)
                        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website service feature created successfully.",
                                websiteServiceFeatureMapper
                                        .toResponse(feature),
                                httpRequest.getRequestURI()
                        )
                );
    }

    /**
     * Updates a feature belonging to a draft service version.
     *
     * PUT /api/v1/admin/website-service-features/{serviceFeatureId}
     */
    @PutMapping("/{serviceFeatureId}")
    public ResponseEntity<
            ApiResponse<WebsiteServiceFeatureResponse>
            > updateServiceFeature(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceFeatureId,

            @Valid
            @RequestBody
            WebsiteServiceFeatureUpdateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteServiceFeature feature =
                websiteServiceFeatureService
                        .updateServiceFeature(
                                serviceFeatureId,
                                websiteServiceFeatureMapper
                                        .toUpdateEntity(request),
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website service feature updated successfully.",
                feature,
                httpRequest
        );
    }

    /**
     * Retrieves one service feature.
     *
     * GET /api/v1/admin/website-service-features/{serviceFeatureId}
     */
    @GetMapping("/{serviceFeatureId}")
    public ResponseEntity<
            ApiResponse<WebsiteServiceFeatureResponse>
            > getServiceFeature(
            @PathVariable
            UUID serviceFeatureId,

            HttpServletRequest httpRequest
    ) {
        return successfulResponse(
                "Website service feature retrieved successfully.",
                websiteServiceFeatureService
                        .getServiceFeature(serviceFeatureId),
                httpRequest
        );
    }

    /**
     * Retrieves all features for one service version.
     *
     * GET /api/v1/admin/website-service-features/version/{serviceVersionId}
     */
    @GetMapping("/version/{serviceVersionId}")
    public ResponseEntity<
            ApiResponse<List<WebsiteServiceFeatureResponse>>
            > getServiceFeatures(
            @PathVariable
            UUID serviceVersionId,

            HttpServletRequest httpRequest
    ) {
        List<WebsiteServiceFeatureResponse> response =
                websiteServiceFeatureService
                        .getServiceFeatures(serviceVersionId)
                        .stream()
                        .map(
                                websiteServiceFeatureMapper
                                        ::toResponse
                        )
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website service features retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Searches features belonging to one service version.
     *
     * GET /api/v1/admin/website-service-features/version/{id}/search
     */
    @GetMapping("/version/{serviceVersionId}/search")
    public ResponseEntity<
            ApiResponse<Page<WebsiteServiceFeatureResponse>>
            > searchServiceFeatures(
            @PathVariable
            UUID serviceVersionId,

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
        Page<WebsiteServiceFeatureResponse> response =
                websiteServiceFeatureService
                        .searchServiceFeatures(
                                serviceVersionId,
                                keyword,
                                isActive,
                                pageable
                        )
                        .map(
                                websiteServiceFeatureMapper
                                        ::toResponse
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website service features retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Updates one feature's active state.
     *
     * PATCH /api/v1/admin/website-service-features/{id}/status
     */
    @PatchMapping("/{serviceFeatureId}/status")
    public ResponseEntity<
            ApiResponse<WebsiteServiceFeatureResponse>
            > updateServiceFeatureStatus(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceFeatureId,

            @Valid
            @RequestBody
            WebsiteServiceFeatureStatusRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteServiceFeature feature =
                websiteServiceFeatureService
                        .updateServiceFeatureStatus(
                                serviceFeatureId,
                                request.isActive(),
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website service-feature status updated successfully.",
                feature,
                httpRequest
        );
    }

    /**
     * Updates one feature's display order.
     *
     * PATCH /api/v1/admin/website-service-features/{id}/order
     */
    @PatchMapping("/{serviceFeatureId}/order")
    public ResponseEntity<
            ApiResponse<WebsiteServiceFeatureResponse>
            > updateServiceFeatureOrder(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceFeatureId,

            @Valid
            @RequestBody
            WebsiteServiceFeatureOrderRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsiteServiceFeature feature =
                websiteServiceFeatureService
                        .updateServiceFeatureOrder(
                                serviceFeatureId,
                                request.displayOrder(),
                                requireAdministratorId(principal)
                        );

        return successfulResponse(
                "Website service-feature order updated successfully.",
                feature,
                httpRequest
        );
    }

    /**
     * Permanently deletes a draft-owned service feature.
     *
     * DELETE /api/v1/admin/website-service-features/{serviceFeatureId}
     */
    @DeleteMapping("/{serviceFeatureId}")
    public ResponseEntity<Void> deleteServiceFeature(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID serviceFeatureId
    ) {
        websiteServiceFeatureService
                .deleteServiceFeature(
                        serviceFeatureId,
                        requireAdministratorId(principal)
                );

        return ResponseEntity.noContent().build();
    }

    /**
     * Counts all features belonging to a service version.
     */
    @GetMapping("/version/{serviceVersionId}/counts/all")
    public ResponseEntity<ApiResponse<Long>>
    countServiceFeatures(
            @PathVariable
            UUID serviceVersionId,

            HttpServletRequest httpRequest
    ) {
        long count =
                websiteServiceFeatureService
                        .countServiceFeatures(
                                serviceVersionId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Website service-feature count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    /**
     * Counts active features belonging to a service version.
     */
    @GetMapping("/version/{serviceVersionId}/counts/active")
    public ResponseEntity<ApiResponse<Long>>
    countActiveServiceFeatures(
            @PathVariable
            UUID serviceVersionId,

            HttpServletRequest httpRequest
    ) {
        long count =
                websiteServiceFeatureService
                        .countActiveServiceFeatures(
                                serviceVersionId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Active service-feature count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
                )
        );
    }

    private ResponseEntity<
            ApiResponse<WebsiteServiceFeatureResponse>
            > successfulResponse(
            String message,
            WebsiteServiceFeature feature,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        message,
                        websiteServiceFeatureMapper
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