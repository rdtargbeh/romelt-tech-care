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
import romelt_techcare.backend.dto.PublicWebsitePricingPlanResponse;
import romelt_techcare.backend.mapper.WebsitePricingPlanMapper;
import romelt_techcare.backend.service.WebsitePricingPlanService;

import java.time.Duration;
import java.util.List;

/**
 * ================================================================
 * ROMELT TECHCARE — PUBLIC PRICING PLAN CONTROLLER
 * ================================================================
 *
 * Purpose:
 * Exposes active, non-deleted pricing-plan identities that have a
 * current published-version pointer.
 *
 * Actual pricing names, amounts, billing periods, descriptions,
 * features, call-to-action content, and recommendation information
 * will be returned by the future pricing-plan version API.
 *
 * Base endpoint:
 * /api/v1/public/website-pricing-plans
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/public/website-pricing-plans")
@RequiredArgsConstructor
public class PublicWebsitePricingPlanController {

    private static final Duration PUBLIC_CACHE_DURATION =
            Duration.ofMinutes(5);

    private final WebsitePricingPlanService
            websitePricingPlanService;

    private final WebsitePricingPlanMapper
            websitePricingPlanMapper;

    @GetMapping
    public ResponseEntity<
            ApiResponse<List<PublicWebsitePricingPlanResponse>>
            > getPublicPricingPlans(
            HttpServletRequest httpRequest
    ) {
        List<PublicWebsitePricingPlanResponse> response =
                websitePricingPlanService
                        .getPublicPricingPlans()
                        .stream()
                        .map(
                                websitePricingPlanMapper
                                        ::toPublicResponse
                        )
                        .toList();

        return listResponse(
                "Public website pricing plans retrieved successfully.",
                response,
                httpRequest
        );
    }

    @GetMapping("/by-code/{planCode}")
    public ResponseEntity<
            ApiResponse<PublicWebsitePricingPlanResponse>
            > getPublicPricingPlanByCode(
            @PathVariable
            String planCode,

            HttpServletRequest httpRequest
    ) {
        PublicWebsitePricingPlanResponse response =
                websitePricingPlanMapper.toPublicResponse(
                        websitePricingPlanService
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
            ApiResponse<PublicWebsitePricingPlanResponse>
            > getPublicPricingPlanBySlug(
            @PathVariable
            String planSlug,

            HttpServletRequest httpRequest
    ) {
        PublicWebsitePricingPlanResponse response =
                websitePricingPlanMapper.toPublicResponse(
                        websitePricingPlanService
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
            ApiResponse<List<PublicWebsitePricingPlanResponse>>
            > listResponse(
            String message,
            List<PublicWebsitePricingPlanResponse> response,
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
            ApiResponse<PublicWebsitePricingPlanResponse>
            > singleResponse(
            String message,
            PublicWebsitePricingPlanResponse response,
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