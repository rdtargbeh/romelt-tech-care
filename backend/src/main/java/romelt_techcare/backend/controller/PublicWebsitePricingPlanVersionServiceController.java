package romelt_techcare.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import romelt_techcare.backend.api.ApiResponse;
import romelt_techcare.backend.dto.PublicWebsitePricingPlanVersionServiceResponse;
import romelt_techcare.backend.mapper.WebsitePricingPlanVersionServiceMapper;
import romelt_techcare.backend.service.WebsitePricingPlanVersionServiceRelationshipService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC PRICING PLAN SERVICE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes active website services included in current published
 * pricing-plan versions.
 *
 * Public filtering:
 * - Pricing plan must be active and not deleted.
 * - Pricing-plan version must be current, published, public, and
 *   effective.
 * - Website service must be active and not deleted.
 * - Website service must have a published content version.
 *
 * Base endpoint:
 * /api/v1/public/website-pricing-plan-services
 * ================================================================
 */
@RestController
@RequestMapping(
        "/api/v1/public/website-pricing-plan-services"
)
@RequiredArgsConstructor
public class PublicWebsitePricingPlanVersionServiceController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsitePricingPlanVersionServiceRelationshipService
            relationshipService;

    private final WebsitePricingPlanVersionServiceMapper
            relationshipMapper;

    @GetMapping("/by-plan-code/{planCode}")
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsitePricingPlanVersionServiceResponse>
                    >
            > getPublicServicesByPlanCode(
            @PathVariable
            String planCode,

            HttpServletRequest httpRequest
    ) {
        List<PublicWebsitePricingPlanVersionServiceResponse>
                response =
                relationshipService
                        .getPublicServicesByPlanCode(planCode)
                        .stream()
                        .map(
                                relationshipMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return publicResponse(
                "Public services included in the pricing plan retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/by-plan-slug/{planSlug}")
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsitePricingPlanVersionServiceResponse>
                    >
            > getPublicServicesByPlanSlug(
            @PathVariable
            String planSlug,

            HttpServletRequest httpRequest
    ) {
        List<PublicWebsitePricingPlanVersionServiceResponse>
                response =
                relationshipService
                        .getPublicServicesByPlanSlug(planSlug)
                        .stream()
                        .map(
                                relationshipMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return publicResponse(
                "Public services included in the pricing plan retrieved successfully.",
                response,
                httpRequest
        );
    }

    private ResponseEntity<
            ApiResponse<
                    List<PublicWebsitePricingPlanVersionServiceResponse>
                    >
            > publicResponse(
            String message,
            List<PublicWebsitePricingPlanVersionServiceResponse>
                    response,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok()
                .cacheControl(
                        CacheControl
                                .maxAge(PUBLIC_CACHE_DURATION)
                                .cachePublic()
                )
                .body(
                        ApiResponse.success(
                                message,
                                response,
                                request.getRequestURI()
                        )
                );
    }
}