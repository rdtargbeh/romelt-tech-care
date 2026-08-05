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
import romelt_techcare.backend.dto.PublicWebsitePricingPlanFeatureResponse;
import romelt_techcare.backend.mapper.WebsitePricingPlanFeatureMapper;
import romelt_techcare.backend.service.WebsitePricingPlanFeatureService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC PRICING PLAN FEATURE CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes active features belonging to currently published website
 * pricing-plan versions.
 *
 * Public filtering:
 * Returned features must belong to:
 * - an active and non-deleted pricing plan;
 * - the pricing plan's current published version;
 * - a PUBLISHED pricing-plan version;
 * - a public pricing-plan version;
 * - a currently effective pricing-plan version.
 *
 * Base endpoint:
 * /api/v1/public/website-pricing-plan-features
 * ================================================================
 */
@RestController
@RequestMapping(
        "/api/v1/public/website-pricing-plan-features"
)
@RequiredArgsConstructor
public class PublicWebsitePricingPlanFeatureController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsitePricingPlanFeatureService
            websitePricingPlanFeatureService;

    private final WebsitePricingPlanFeatureMapper
            websitePricingPlanFeatureMapper;

    @GetMapping("/by-code/{planCode}")
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsitePricingPlanFeatureResponse>
                    >
            > getPublicFeaturesByPlanCode(
            @PathVariable
            String planCode,

            HttpServletRequest httpRequest
    ) {
        List<PublicWebsitePricingPlanFeatureResponse> response =
                websitePricingPlanFeatureService
                        .getPublicFeaturesByPlanCode(planCode)
                        .stream()
                        .map(
                                websitePricingPlanFeatureMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return publicResponse(
                "Public website pricing-plan features retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/by-slug/{planSlug}")
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsitePricingPlanFeatureResponse>
                    >
            > getPublicFeaturesByPlanSlug(
            @PathVariable
            String planSlug,

            HttpServletRequest httpRequest
    ) {
        List<PublicWebsitePricingPlanFeatureResponse> response =
                websitePricingPlanFeatureService
                        .getPublicFeaturesByPlanSlug(planSlug)
                        .stream()
                        .map(
                                websitePricingPlanFeatureMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return publicResponse(
                "Public website pricing-plan features retrieved successfully.",
                response,
                httpRequest
        );
    }

    private ResponseEntity<
            ApiResponse<
                    List<PublicWebsitePricingPlanFeatureResponse>
                    >
            > publicResponse(
            String message,
            List<PublicWebsitePricingPlanFeatureResponse> response,
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