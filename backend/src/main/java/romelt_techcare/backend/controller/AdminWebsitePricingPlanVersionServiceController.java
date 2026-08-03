package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.AdminJwtPrincipal;
import romelt_techcare.backend.dto.WebsitePricingPlanVersionServiceBulkRequest;
import romelt_techcare.backend.dto.WebsitePricingPlanVersionServiceCreateRequest;
import romelt_techcare.backend.dto.WebsitePricingPlanVersionServiceResponse;
import romelt_techcare.backend.entity.WebsitePricingPlanVersionService;
import romelt_techcare.backend.mapper.WebsitePricingPlanVersionServiceMapper;
import romelt_techcare.backend.service.WebsitePricingPlanVersionServiceRelationshipService;

import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 * ROMELT TECHCARE — ADMIN PRICING PLAN VERSION SERVICE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes administrator endpoints for assigning website services to
 * draft pricing-plan versions.
 *
 * Responsibilities:
 * - Adds individual services.
 * - Adds multiple services.
 * - Replaces complete service membership.
 * - Removes individual services.
 * - Clears all service relationships.
 * - Retrieves assigned services.
 * - Retrieves pricing-plan version usage for one service.
 *
 * Mutation rules:
 * Only the current draft pricing-plan version may be changed.
 *
 * Base endpoint:
 * /api/v1/admin/website-pricing-plan-version-services
 * ================================================================
 */
@RestController
@RequestMapping(
        "/api/v1/admin/website-pricing-plan-version-services"
)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminWebsitePricingPlanVersionServiceController {

    private final WebsitePricingPlanVersionServiceRelationshipService
            relationshipService;

    private final WebsitePricingPlanVersionServiceMapper
            relationshipMapper;

    @PostMapping("/version/{pricingPlanVersionId}")
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanVersionServiceResponse>
            > addService(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanVersionId,

            @Valid
            @RequestBody
            WebsitePricingPlanVersionServiceCreateRequest request,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlanVersionService relationship =
                relationshipService.addService(
                        pricingPlanVersionId,
                        request.serviceId(),
                        requireAdministratorId(principal)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website service added to the pricing-plan version.",
                                relationshipMapper.toResponse(
                                        relationship
                                ),
                                httpRequest.getRequestURI()
                        )
                );
    }

    @PostMapping("/version/{pricingPlanVersionId}/bulk")
    public ResponseEntity<
            ApiResponse<
                    List<WebsitePricingPlanVersionServiceResponse>
                    >
            > addServices(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanVersionId,

            @Valid
            @RequestBody
            WebsitePricingPlanVersionServiceBulkRequest request,

            HttpServletRequest httpRequest
    ) {
        List<WebsitePricingPlanVersionServiceResponse> response =
                relationshipService
                        .addServices(
                                pricingPlanVersionId,
                                request.serviceIds(),
                                requireAdministratorId(principal)
                        )
                        .stream()
                        .map(relationshipMapper::toResponse)
                        .toList();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Website services added to the pricing-plan version.",
                                response,
                                httpRequest.getRequestURI()
                        )
                );
    }

    @PutMapping("/version/{pricingPlanVersionId}")
    public ResponseEntity<
            ApiResponse<
                    List<WebsitePricingPlanVersionServiceResponse>
                    >
            > replaceServices(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanVersionId,

            @Valid
            @RequestBody
            WebsitePricingPlanVersionServiceBulkRequest request,

            HttpServletRequest httpRequest
    ) {
        List<WebsitePricingPlanVersionServiceResponse> response =
                relationshipService
                        .replaceServices(
                                pricingPlanVersionId,
                                request.serviceIds(),
                                requireAdministratorId(principal)
                        )
                        .stream()
                        .map(relationshipMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Pricing-plan version service membership replaced successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/version/{pricingPlanVersionId}")
    public ResponseEntity<
            ApiResponse<
                    List<WebsitePricingPlanVersionServiceResponse>
                    >
            > getServicesForPricingPlanVersion(
            @PathVariable
            UUID pricingPlanVersionId,

            HttpServletRequest httpRequest
    ) {
        List<WebsitePricingPlanVersionServiceResponse> response =
                relationshipService
                        .getServicesForPricingPlanVersion(
                                pricingPlanVersionId
                        )
                        .stream()
                        .map(relationshipMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Pricing-plan version services retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping(
            "/version/{pricingPlanVersionId}/service/{serviceId}"
    )
    public ResponseEntity<
            ApiResponse<WebsitePricingPlanVersionServiceResponse>
            > getRelationship(
            @PathVariable
            UUID pricingPlanVersionId,

            @PathVariable
            UUID serviceId,

            HttpServletRequest httpRequest
    ) {
        WebsitePricingPlanVersionService relationship =
                relationshipService.getRelationship(
                        pricingPlanVersionId,
                        serviceId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Pricing-plan version service relationship retrieved successfully.",
                        relationshipMapper.toResponse(
                                relationship
                        ),
                        httpRequest.getRequestURI()
                )
        );
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<
            ApiResponse<
                    List<WebsitePricingPlanVersionServiceResponse>
                    >
            > getPricingPlanVersionsForService(
            @PathVariable
            UUID serviceId,

            HttpServletRequest httpRequest
    ) {
        List<WebsitePricingPlanVersionServiceResponse> response =
                relationshipService
                        .getPricingPlanVersionsForService(
                                serviceId
                        )
                        .stream()
                        .map(relationshipMapper::toResponse)
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Pricing-plan versions for the website service retrieved successfully.",
                        response,
                        httpRequest.getRequestURI()
                )
        );
    }

    @DeleteMapping(
            "/version/{pricingPlanVersionId}/service/{serviceId}"
    )
    public ResponseEntity<Void> removeService(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanVersionId,

            @PathVariable
            UUID serviceId
    ) {
        relationshipService.removeService(
                pricingPlanVersionId,
                serviceId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/version/{pricingPlanVersionId}")
    public ResponseEntity<Void> clearServices(
            @AuthenticationPrincipal
            AdminJwtPrincipal principal,

            @PathVariable
            UUID pricingPlanVersionId
    ) {
        relationshipService.clearServices(
                pricingPlanVersionId,
                requireAdministratorId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/version/{pricingPlanVersionId}/count")
    public ResponseEntity<ApiResponse<Long>>
    countServicesForPricingPlanVersion(
            @PathVariable
            UUID pricingPlanVersionId,

            HttpServletRequest httpRequest
    ) {
        long count =
                relationshipService
                        .countServicesForPricingPlanVersion(
                                pricingPlanVersionId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Pricing-plan version service count retrieved successfully.",
                        count,
                        httpRequest.getRequestURI()
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