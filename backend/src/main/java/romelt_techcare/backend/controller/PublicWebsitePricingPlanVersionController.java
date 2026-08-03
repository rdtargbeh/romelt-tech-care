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
import romelt_techcare.backend.dto.PublicWebsitePricingPlanVersionResponse;
import romelt_techcare.backend.mapper.WebsitePricingPlanVersionMapper;
import romelt_techcare.backend.service.WebsitePricingPlanVersionService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC PRICING PLAN CONTENT CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes current published pricing-plan content to the public React
 * website.
 *
 * Public filtering:
 * Returned plans must be:
 * - active and non-deleted;
 * - currently published;
 * - marked public;
 * - inside the effective publication window.
 *
 * Base endpoint:
 * /api/v1/public/website-pricing-plan-content
 * ================================================================
 */
@RestController
@RequestMapping(
        "/api/v1/public/website-pricing-plan-content"
)
@RequiredArgsConstructor
public class PublicWebsitePricingPlanVersionController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsitePricingPlanVersionService
            websitePricingPlanVersionService;

    private final WebsitePricingPlanVersionMapper
            websitePricingPlanVersionMapper;

    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsitePricingPlanVersionResponse>
                    >
            > getPublicPricingPlans(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsitePricingPlanVersionResponse> response =
                websitePricingPlanVersionService
                        .getPublicPricingPlans()
                        .stream()
                        .map(
                                websitePricingPlanVersionMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return listResponse(
                "Public website pricing plans retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/featured")
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsitePricingPlanVersionResponse>
                    >
            > getFeaturedPublicPricingPlans(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsitePricingPlanVersionResponse> response =
                websitePricingPlanVersionService
                        .getFeaturedPublicPricingPlans()
                        .stream()
                        .map(
                                websitePricingPlanVersionMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return listResponse(
                "Featured public pricing plans retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/recommended")
    public ResponseEntity<
            ApiResponse<
                    List<PublicWebsitePricingPlanVersionResponse>
                    >
            > getRecommendedPublicPricingPlans(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsitePricingPlanVersionResponse> response =
                websitePricingPlanVersionService
                        .getRecommendedPublicPricingPlans()
                        .stream()
                        .map(
                                websitePricingPlanVersionMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return listResponse(
                "Recommended public pricing plans retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/by-code/{planCode}")
    public ResponseEntity<
            ApiResponse<PublicWebsitePricingPlanVersionResponse>
            > getPublicPricingPlanByCode(
            @PathVariable
            String planCode,

            HttpServletRequest httpRequest
    ) {
        PublicWebsitePricingPlanVersionResponse response =
                websitePricingPlanVersionMapper
                        .toPublicResponse(
                                websitePricingPlanVersionService
                                        .getPublicPricingPlanByCode(
                                                planCode
                                        )
                        );

        return singleResponse(
                "Public website pricing plan retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/by-slug/{planSlug}")
    public ResponseEntity<
            ApiResponse<PublicWebsitePricingPlanVersionResponse>
            > getPublicPricingPlanBySlug(
            @PathVariable
            String planSlug,

            HttpServletRequest httpRequest
    ) {
        PublicWebsitePricingPlanVersionResponse response =
                websitePricingPlanVersionMapper
                        .toPublicResponse(
                                websitePricingPlanVersionService
                                        .getPublicPricingPlanBySlug(
                                                planSlug
                                        )
                        );

        return singleResponse(
                "Public website pricing plan retrieved successfully.",
                response,
                httpRequest
        );
    }

    private ResponseEntity<
            ApiResponse<
                    List<PublicWebsitePricingPlanVersionResponse>
                    >
            > listResponse(
            String message,
            List<PublicWebsitePricingPlanVersionResponse> response,
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

    private ResponseEntity<
            ApiResponse<PublicWebsitePricingPlanVersionResponse>
            > singleResponse(
            String message,
            PublicWebsitePricingPlanVersionResponse response,
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